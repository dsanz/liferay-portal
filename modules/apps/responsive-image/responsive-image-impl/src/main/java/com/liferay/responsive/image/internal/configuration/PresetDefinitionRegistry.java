/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.responsive.image.constants.ResponsiveImagePresetConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the presets a scope has, and hands one back by name.
 *
 * <p>
 * Owns three things a caller should not have to know about: the built in
 * presets, the parsing of the configured entries layered over them, and the
 * memo that keeps both off the rendering path. What is left is a name in and a
 * definition out, which is why {@link #getPresetDefinition} always answers,
 * falling back rather than reporting that a name is unknown.
 * </p>
 *
 * <p>
 * Presets are framework owned rather than provider owned, so that layout
 * is described once regardless of which provider is serving images. Providers
 * consume this; they do not define it.
 * </p>
 *
 * <p>
 * Configuration is instance scoped, so the same preset name can resolve
 * differently for two companies in the same JVM.
 * </p>
 *
 * @author Daniel Sanz
 */
public class PresetDefinitionRegistry {

	public PresetDefinitionRegistry(
		ConfigurationProvider configurationProvider, Portal portal) {

		_responsiveImageConfigurationHelper =
			new ResponsiveImageConfigurationHelper(
				configurationProvider, portal);
	}

	/**
	 * Returns the preset of the given name, never <code>null</code>.
	 *
	 * <p>
	 * A blank name and a name nobody declared both answer with the scope's
	 * own default preset, because they are the same situation: no usable
	 * preset was named. A typo in configuration should cost an optimization,
	 * not the image. A name published in {@code
	 * ResponsiveImagePresetConstants} never takes that path: those are seeded
	 * before any configuration is read, which is also why the default is
	 * always there to fall back to.
	 * </p>
	 *
	 * @param  groupId the site being rendered for, or <code>0</code>
	 * @param  companyId the company whose configuration applies
	 * @param  presetName the preset name, or <code>null</code> for the
	 *         default
	 * @return the preset definition, never <code>null</code>
	 */
	public PresetDefinition getPresetDefinition(
		long groupId, long companyId, String presetName) {

		if (Validator.isBlank(presetName)) {
			presetName = ResponsiveImagePresetConstants.DEFAULT;
		}

		Map<String, PresetDefinition> presetDefinitions = _getPresetDefinitions(
			groupId, companyId);

		PresetDefinition presetDefinition = presetDefinitions.get(presetName);

		if (presetDefinition != null) {
			return presetDefinition;
		}

		if (_log.isDebugEnabled()) {
			_log.debug("No preset named " + presetName + ", using the default");
		}

		// A name nobody declared is the same situation as no name at all, so
		// both end up on the scope's own default rather than on two different
		// answers. It is always present, being built in.

		return presetDefinitions.get(ResponsiveImagePresetConstants.DEFAULT);
	}

	/**
	 * Writes the built in presets into the maps the parser fills, before it
	 * reads a single configured entry.
	 *
	 * <p>
	 * Seeding the properties rather than finished {@link PresetDefinition}
	 * objects is what keeps a configured key overriding one field and leaving
	 * the rest. A built definition cannot express "unset": merging two of them
	 * would turn a site that only changed <code>sizes</code> into one that also
	 * silently turned automatic sizing off.
	 * </p>
	 */
	private static void _seedBuiltInPresets(
		Map<String, String> labels, Map<String, Boolean> lazyValues,
		Map<String, Map<String, Map<String, String>>> presetProperties) {

		for (BuiltInPreset builtInPreset : _BUILT_IN_PRESETS) {
			labels.put(builtInPreset._name, builtInPreset._label);
			lazyValues.put(builtInPreset._name, builtInPreset._lazy);

			Map<String, Map<String, String>> sourceProperties =
				presetProperties.computeIfAbsent(
					builtInPreset._name, name -> new LinkedHashMap<>());

			Map<String, String> properties = sourceProperties.computeIfAbsent(
				_MEDIA_CONDITION_DEFAULT, name -> new LinkedHashMap<>());

			properties.put(_SIZES, builtInPreset._sizes);

			if (builtInPreset._autoSizes) {
				properties.put(_AUTO_SIZES, StringPool.TRUE);
			}

			if (builtInPreset._maxWidth != null) {
				properties.put(
					_MAX_WIDTH, String.valueOf(builtInPreset._maxWidth));
			}
		}
	}

	private static Integer _toMaxWidth(String value) {
		if (Validator.isBlank(value)) {
			return null;
		}

		int maxWidth = GetterUtil.getInteger(value);

		if (maxWidth > 0) {
			return maxWidth;
		}

		if (_log.isWarnEnabled()) {
			_log.warn("Ignoring invalid maximum width " + value);
		}

		return null;
	}

	private static Map<String, PresetDefinition> _toPresetDefinitions(
		Map<String, String> mediaConditions, String[] presets) {

		Map<String, String> labels = new LinkedHashMap<>();
		Map<String, Boolean> lazyValues = new LinkedHashMap<>();
		Map<String, Map<String, Map<String, String>>> presetProperties =
			new LinkedHashMap<>();

		_seedBuiltInPresets(labels, lazyValues, presetProperties);

		if (presets == null) {
			presets = new String[0];
		}

		for (String preset : presets) {
			if (Validator.isBlank(preset)) {
				continue;
			}

			int i = preset.indexOf(StringPool.EQUAL);

			if (i <= 0) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring malformed preset entry " + preset);
				}

				continue;
			}

			String key = StringUtil.trim(preset.substring(0, i));
			String value = StringUtil.trim(preset.substring(i + 1));

			String[] keyParts = StringUtil.split(key, StringPool.PERIOD);

			if ((keyParts.length == 2) && _LABEL.equals(keyParts[1])) {
				labels.put(keyParts[0], value);

				continue;
			}

			if ((keyParts.length == 2) && _LAZY.equals(keyParts[1])) {
				lazyValues.put(keyParts[0], GetterUtil.getBoolean(value));

				continue;
			}

			if (keyParts.length != 3) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring unrecognized preset key " + key);
				}

				continue;
			}

			if (!_MEDIA_CONDITION_DEFAULT.equals(keyParts[1]) &&
				!mediaConditions.containsKey(keyParts[1])) {

				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Ignoring preset key ", key,
							" because no media condition named ", keyParts[1],
							" is declared"));
				}

				continue;
			}

			Map<String, Map<String, String>> sourceProperties =
				presetProperties.computeIfAbsent(
					keyParts[0], presetName -> new LinkedHashMap<>());

			Map<String, String> properties = sourceProperties.computeIfAbsent(
				keyParts[1], mediaConditionName -> new LinkedHashMap<>());

			properties.put(keyParts[2], value);
		}

		Map<String, PresetDefinition> presetDefinitions = new LinkedHashMap<>();

		for (Map.Entry<String, Map<String, Map<String, String>>> entry :
				presetProperties.entrySet()) {

			presetDefinitions.put(
				entry.getKey(),
				new PresetDefinition(
					labels.get(entry.getKey()), lazyValues.get(entry.getKey()),
					entry.getKey(),
					_toSourceDefinitions(mediaConditions, entry.getValue())));
		}

		return presetDefinitions;
	}

	/**
	 * Orders a preset's source definitions by the order media conditions are
	 * declared rather than by the order the preset's own keys appear, so
	 * ordering is decided once for the whole installation. The unconditional
	 * media condition always sorts last, being the catch all.
	 */
	private static List<SourceDefinition> _toSourceDefinitions(
		Map<String, String> mediaConditions,
		Map<String, Map<String, String>> sourceProperties) {

		List<SourceDefinition> sourceDefinitions = new ArrayList<>(
			sourceProperties.size());

		for (Map.Entry<String, String> entry : mediaConditions.entrySet()) {
			Map<String, String> properties = sourceProperties.get(
				entry.getKey());

			if (properties == null) {
				continue;
			}

			sourceDefinitions.add(
				new SourceDefinition(
					GetterUtil.getBoolean(properties.get(_AUTO_SIZES)),
					entry.getKey(), _toMaxWidth(properties.get(_MAX_WIDTH)),
					entry.getValue(), properties.get(_SIZES),
					_toTransformations(properties.get(_TRANSFORMATIONS))));
		}

		Map<String, String> properties = sourceProperties.get(
			_MEDIA_CONDITION_DEFAULT);

		if (properties != null) {
			sourceDefinitions.add(
				new SourceDefinition(
					GetterUtil.getBoolean(properties.get(_AUTO_SIZES)),
					_MEDIA_CONDITION_DEFAULT,
					_toMaxWidth(properties.get(_MAX_WIDTH)), null,
					properties.get(_SIZES),
					_toTransformations(properties.get(_TRANSFORMATIONS))));
		}

		return sourceDefinitions;
	}

	private static Map<String, String> _toTransformations(String value) {
		if (Validator.isBlank(value)) {
			return Collections.emptyMap();
		}

		Map<String, String> transformations = new LinkedHashMap<>();

		for (String entry : StringUtil.split(value, StringPool.COMMA)) {
			int i = entry.indexOf(StringPool.EQUAL);

			if (i <= 0) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring malformed transformation " + entry);
				}

				continue;
			}

			transformations.put(
				StringUtil.trim(entry.substring(0, i)),
				StringUtil.trim(entry.substring(i + 1)));
		}

		return transformations;
	}

	/**
	 * Returns the parsed presets for a company, reparsing only when the
	 * underlying configuration has actually changed.
	 *
	 * <p>
	 * Memoized on the configuration's own contents rather than invalidated by
	 * an event: a stale cache would silently serve the previous layout after an
	 * administrator edited it, and there is no reliable notification to hang
	 * invalidation on.
	 * </p>
	 */
	private Map<String, PresetDefinition> _getPresetDefinitions(
		long groupId, long companyId) {

		ResponsiveImageConfiguration responsiveImageConfiguration =
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				groupId, companyId);

		// Unreadable configuration must not take the built in presets with it.
		// A component naming one is entitled to an answer precisely when
		// something else has gone wrong.

		if (responsiveImageConfiguration == null) {
			return _builtInPresetDefinitions;
		}

		String[] mediaConditions =
			responsiveImageConfiguration.mediaConditions();
		String[] presets = responsiveImageConfiguration.presets();

		int contentHash =
			(31 * Arrays.hashCode(mediaConditions)) + Arrays.hashCode(presets);

		long scopeKey = _responsiveImageConfigurationHelper.getScopeKey(
			groupId, companyId);

		ScopedPresetDefinitions scopedPresetDefinitions =
			_scopedPresetDefinitions.get(scopeKey);

		if ((scopedPresetDefinitions != null) &&
			(scopedPresetDefinitions._contentHash == contentHash)) {

			return scopedPresetDefinitions._presetDefinitions;
		}

		scopedPresetDefinitions = new ScopedPresetDefinitions(
			contentHash,
			_toPresetDefinitions(_toMediaConditions(mediaConditions), presets));

		_scopedPresetDefinitions.put(scopeKey, scopedPresetDefinitions);

		_report(scopeKey, scopedPresetDefinitions._presetDefinitions);

		return scopedPresetDefinitions._presetDefinitions;
	}

	/**
	 * Logs what the configuration renders and anything wrong with it.
	 *
	 * <p>
	 * Reached only when the configuration has actually changed, which is both
	 * when an administrator is in a position to act on it and the only time it
	 * says anything new. Reporting per request would bury it.
	 * </p>
	 */
	private void _report(
		long scopeKey, Map<String, PresetDefinition> presetDefinitions) {

		if (_log.isWarnEnabled()) {
			for (String problem :
					ResponsiveImageConfigurationValidator.validate(
						presetDefinitions)) {

				_log.warn(problem);
			}
		}

		if (!_log.isDebugEnabled()) {
			return;
		}

		for (PresetDefinition presetDefinition : presetDefinitions.values()) {
			_log.debug(
				StringBundler.concat(
					"Preset ", presetDefinition.getName(), " of scope ",
					scopeKey, " renders ",
					ResponsiveImageConfigurationValidator.getMarkupShape(
						presetDefinition)));
		}
	}

	/**
	 * Returns the declared media conditions by name, in declaration order.
	 *
	 * <p>
	 * That order is what fixes the order presets render in. Browser source
	 * matching is first wins, so reordering this configuration renders
	 * different images without reporting anything.
	 * </p>
	 */
	private Map<String, String> _toMediaConditions(
		String[] mediaConditionEntries) {

		Map<String, String> mediaConditions = new LinkedHashMap<>();

		if (mediaConditionEntries == null) {
			return mediaConditions;
		}

		for (String mediaConditionEntry : mediaConditionEntries) {
			if (Validator.isBlank(mediaConditionEntry)) {
				continue;
			}

			int i = mediaConditionEntry.indexOf(StringPool.EQUAL);

			if (i <= 0) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Ignoring malformed media condition entry " +
							mediaConditionEntry);
				}

				continue;
			}

			String key = StringUtil.trim(mediaConditionEntry.substring(0, i));

			String[] keyParts = StringUtil.split(key, StringPool.PERIOD);

			if ((keyParts.length != 2) || !_MEDIA.equals(keyParts[1])) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Ignoring unrecognized media condition key " + key);
				}

				continue;
			}

			mediaConditions.put(
				keyParts[0],
				StringUtil.trim(mediaConditionEntry.substring(i + 1)));
		}

		return mediaConditions;
	}

	private static final String _AUTO_SIZES = "autoSizes";

	/**
	 * The presets every installation has, whatever is configured.
	 *
	 * <p>
	 * Parsed ahead of the configured entries, so a configured key of the same
	 * name overwrites the built in value and every key left unset keeps it. A
	 * site that only wants a different <code>sizes</code> for cards writes that
	 * one entry and still inherits the label, the loading behavior and the
	 * maximum width.
	 * </p>
	 *
	 * <p>
	 * They cannot be removed, because a list of <code>key=value</code> entries
	 * has no way to spell a removal. That is the point: a component rendering a
	 * card can name the preset without first checking whether someone deleted
	 * it, and without a silent fall back to a different size. Hiding one from an
	 * authoring UI is a separate concern from deleting it and needs its own key.
	 * </p>
	 *
	 * <p>
	 * All of them are unconditional. Art direction means several media
	 * conditions, which are declared per installation, so a built in preset
	 * naming one would be dropped wherever that name is not declared.
	 * </p>
	 */
	private static final BuiltInPreset[] _BUILT_IN_PRESETS = {
		new BuiltInPreset(
			"Default", null, ResponsiveImagePresetConstants.DEFAULT, "100vw"
		).autoSizes(),
		new BuiltInPreset(
			"Thumbnail", 320, ResponsiveImagePresetConstants.THUMBNAIL, "96px"),
		new BuiltInPreset(
			"Card", 960, ResponsiveImagePresetConstants.CARD,
			"(min-width: 992px) 25vw, 100vw"),
		new BuiltInPreset(
			"Content", 1440, ResponsiveImagePresetConstants.CONTENT, "100vw"),
		new BuiltInPreset(
			"Hero", 2160, ResponsiveImagePresetConstants.HERO, "100vw"
		).eager(),
		new BuiltInPreset(
			"Full Width", 2880, ResponsiveImagePresetConstants.FULL_WIDTH,
			"100vw"
		).eager()
	};

	private static final String _LABEL = "label";

	private static final String _LAZY = "lazy";

	private static final String _MAX_WIDTH = "maxWidth";

	private static final String _MEDIA = "media";

	private static final String _MEDIA_CONDITION_DEFAULT = "default";

	private static final String _SIZES = "sizes";

	private static final String _TRANSFORMATIONS = "transformations";

	private static final Log _log = LogFactoryUtil.getLog(
		PresetDefinitionRegistry.class);

	/**
	 * The built in presets alone, with nothing configured.
	 *
	 * <p>
	 * They are the same for every company and every site, so they are built
	 * once rather than per scope, and they are what a scope with no readable
	 * configuration resolves against.
	 * </p>
	 */
	private static final Map<String, PresetDefinition>
		_builtInPresetDefinitions;

	static {

		// Unmodifiable because this one map is handed to every scope for the
		// life of the process, unlike the per scope maps beside it.

		_builtInPresetDefinitions = Collections.unmodifiableMap(
			_toPresetDefinitions(Collections.emptyMap(), new String[0]));
	}

	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private final Map<Long, ScopedPresetDefinitions> _scopedPresetDefinitions =
		new ConcurrentHashMap<>();

	/**
	 * One row of the built in table.
	 *
	 * <p>
	 * Deliberately not a {@link PresetDefinition}. This carries only what a
	 * built in sets, so that everything it leaves out stays genuinely unset
	 * and a configured entry can fill it in.
	 * </p>
	 *
	 * <p>
	 * Lazy loading and automatic sizing are set by name rather than passed in,
	 * because the two are booleans and Liferay orders parameters
	 * alphabetically, which would put them next to each other in a table that
	 * is read far more often than it is written.
	 * </p>
	 */
	private static class BuiltInPreset {

		public BuiltInPreset(
			String label, Integer maxWidth, String name, String sizes) {

			_label = label;
			_maxWidth = maxWidth;
			_name = name;
			_sizes = sizes;
		}

		public BuiltInPreset autoSizes() {
			_autoSizes = true;

			return this;
		}

		public BuiltInPreset eager() {
			_lazy = false;

			return this;
		}

		private boolean _autoSizes;
		private final String _label;
		private boolean _lazy = true;
		private final Integer _maxWidth;
		private final String _name;
		private final String _sizes;

	}

	private static class ScopedPresetDefinitions {

		private ScopedPresetDefinitions(
			int contentHash, Map<String, PresetDefinition> presetDefinitions) {

			_contentHash = contentHash;
			_presetDefinitions = presetDefinitions;
		}

		private final int _contentHash;
		private final Map<String, PresetDefinition> _presetDefinitions;

	}

}