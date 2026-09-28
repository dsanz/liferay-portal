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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the presets a scope has, and hands one back by name.
 *
 * @author Daniel Sanz
 */
public class ResponsiveImageConfigurationRegistry {

	public ResponsiveImageConfigurationRegistry(
		ConfigurationProvider configurationProvider, Portal portal) {

		_responsiveImageConfigurationHelper =
			new ResponsiveImageConfigurationHelper(
				configurationProvider, portal);
	}

	/**
	 * Returns the preset of the given name, never <code>null</code>.
	 *
	 * @param  groupId the site being rendered for, or <code>0</code>
	 * @param  companyId the company whose configuration applies
	 * @param  presetName the preset name, or <code>null</code> for the
	 *         default
	 * @return the preset definition, never <code>null</code>
	 */
	public PresetDefinition getPresetDefinition(
		long groupId, long companyId, String presetName) {

		ScopedConfiguration scopedConfiguration = getScopedConfiguration(
			groupId, companyId);

		return scopedConfiguration.getPresetDefinition(presetName);
	}

	/**
	 * Returns this scope's whole parsed configuration, reparsing only when it
	 * has actually changed.
	 */
	public ScopedConfiguration getScopedConfiguration(
		long groupId, long companyId) {

		ResponsiveImageConfiguration responsiveImageConfiguration =
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				groupId, companyId);

		if (responsiveImageConfiguration == null) {
			return BuiltInScopedConfigurationHolder._scopedConfiguration;
		}

		String[] candidateWidths =
			responsiveImageConfiguration.candidateWidths();
		String[] defaultTransformations =
			responsiveImageConfiguration.defaultTransformations();
		String[] mediaConditions =
			responsiveImageConfiguration.mediaConditions();
		String[] presets = responsiveImageConfiguration.presets();
		String urlRendererName = responsiveImageConfiguration.urlRendererName();

		int contentHash = Objects.hash(
			Arrays.hashCode(candidateWidths),
			Arrays.hashCode(defaultTransformations),
			Arrays.hashCode(mediaConditions), Arrays.hashCode(presets),
			urlRendererName);

		long scopeKey = _responsiveImageConfigurationHelper.getScopeKey(
			groupId, companyId);

		ScopedConfiguration scopedConfiguration = _scopedConfigurations.get(
			scopeKey);

		if ((scopedConfiguration != null) &&
			(scopedConfiguration._getContentHash() == contentHash)) {

			return scopedConfiguration;
		}

		scopedConfiguration = new ScopedConfiguration(
			_toWidths(candidateWidths), contentHash,
			_toEntries(defaultTransformations),
			_toPresetDefinitions(_toMediaConditions(mediaConditions), presets),
			urlRendererName);

		_scopedConfigurations.put(scopeKey, scopedConfiguration);

		_report(scopeKey, scopedConfiguration._getPresetDefinitions());

		return scopedConfiguration;
	}

	public static class ScopedConfiguration {

		public TreeSet<Integer> getCandidateWidths() {
			return _candidateWidths;
		}

		public Map<String, String> getDefaultTransformations() {
			return _defaultTransformations;
		}

		public PresetDefinition getPresetDefinition(String presetName) {
			if (Validator.isBlank(presetName)) {
				presetName = ResponsiveImagePresetConstants.DEFAULT;
			}

			PresetDefinition presetDefinition = _presetDefinitions.get(
				presetName);

			if (presetDefinition != null) {
				return presetDefinition;
			}

			if (_log.isDebugEnabled()) {
				_log.debug(
					"No preset named " + presetName + ", using the default");
			}

			return _presetDefinitions.get(
				ResponsiveImagePresetConstants.DEFAULT);
		}

		public String getURLRendererName() {
			return _urlRendererName;
		}

		private ScopedConfiguration(
			TreeSet<Integer> candidateWidths, int contentHash,
			Map<String, String> defaultTransformations,
			Map<String, PresetDefinition> presetDefinitions,
			String urlRendererName) {

			_candidateWidths = candidateWidths;
			_contentHash = contentHash;
			_defaultTransformations = defaultTransformations;
			_presetDefinitions = presetDefinitions;
			_urlRendererName = urlRendererName;
		}

		private int _getContentHash() {
			return _contentHash;
		}

		private Map<String, PresetDefinition> _getPresetDefinitions() {
			return _presetDefinitions;
		}

		private final TreeSet<Integer> _candidateWidths;
		private final int _contentHash;
		private final Map<String, String> _defaultTransformations;
		private final Map<String, PresetDefinition> _presetDefinitions;
		private final String _urlRendererName;

	}

	private static void _addBuiltInPresets(
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

		_addBuiltInPresets(labels, lazyValues, presetProperties);

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
			List<SourceDefinition> sourceDefinitions =
				presetDefinition.getSourceDefinitions();

			_log.debug(
				StringBundler.concat(
					"Preset ", presetDefinition.getName(), " of scope ",
					scopeKey, " declares ", sourceDefinitions.size(),
					" media conditions"));
		}
	}

	private Map<String, String> _toEntries(String[] entries) {
		if (entries == null) {
			return Collections.emptyMap();
		}

		Map<String, String> map = new HashMap<>();

		for (String entry : entries) {
			if (Validator.isBlank(entry)) {
				continue;
			}

			int i = entry.indexOf(StringPool.EQUAL);

			if (i <= 0) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring malformed entry " + entry);
				}

				continue;
			}

			map.put(entry.substring(0, i), entry.substring(i + 1));
		}

		return map;
	}

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

	private TreeSet<Integer> _toWidths(String[] candidateWidths) {
		TreeSet<Integer> widths = new TreeSet<>();

		if (candidateWidths == null) {
			return widths;
		}

		for (String candidateWidth : candidateWidths) {
			int width = GetterUtil.getInteger(candidateWidth);

			if (width > 0) {
				widths.add(width);
			}
			else if (_log.isWarnEnabled()) {
				_log.warn("Ignoring invalid candidate width " + candidateWidth);
			}
		}

		return widths;
	}

	private static final String _AUTO_SIZES = "autoSizes";

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
		ResponsiveImageConfigurationRegistry.class);

	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private final Map<Long, ScopedConfiguration> _scopedConfigurations =
		new ConcurrentHashMap<>();

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

	private static class BuiltInScopedConfigurationHolder {

		private static final ScopedConfiguration _scopedConfiguration =
			new ScopedConfiguration(
				new TreeSet<>(), 0, Collections.<String, String>emptyMap(),
				Collections.unmodifiableMap(
					_toPresetDefinitions(
						Collections.emptyMap(), new String[0])),
				null);

	}

}