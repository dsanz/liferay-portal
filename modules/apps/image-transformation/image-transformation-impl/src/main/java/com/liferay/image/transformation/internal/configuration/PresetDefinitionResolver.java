/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a preset name against configuration.
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
public class PresetDefinitionResolver {

	public PresetDefinitionResolver(
		ConfigurationProvider configurationProvider, Portal portal) {

		_imageTransformationConfigurationHelper =
			new ImageTransformationConfigurationHelper(
				configurationProvider, portal);
	}

	public PresetDefinition resolve(
		long groupId, long companyId, String presetName) {

		if (Validator.isBlank(presetName)) {
			presetName = _NAME_DEFAULT;
		}

		Map<String, PresetDefinition> presetDefinitions = _getPresetDefinitions(
			groupId, companyId);

		PresetDefinition presetDefinition = presetDefinitions.get(presetName);

		if (presetDefinition != null) {
			return presetDefinition;
		}

		if (_log.isDebugEnabled()) {
			_log.debug("No preset named " + presetName + ", using fallback");
		}

		return _FALLBACK;
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

		ImageTransformationConfiguration imageTransformationConfiguration =
			_imageTransformationConfigurationHelper.
				getImageTransformationConfiguration(groupId, companyId);

		if (imageTransformationConfiguration == null) {
			return Collections.emptyMap();
		}

		String[] breakpoints = imageTransformationConfiguration.breakpoints();
		String[] presets = imageTransformationConfiguration.presets();

		int contentHash =
			(31 * Arrays.hashCode(breakpoints)) + Arrays.hashCode(presets);

		long scopeKey = _imageTransformationConfigurationHelper.getScopeKey(
			groupId, companyId);

		ParsedPresets parsedPresets = _parsedPresets.get(scopeKey);

		if ((parsedPresets != null) &&
			(parsedPresets._contentHash == contentHash)) {

			return parsedPresets._presetDefinitions;
		}

		parsedPresets = new ParsedPresets(
			contentHash,
			_toPresetDefinitions(_toBreakpoints(breakpoints), presets));

		_parsedPresets.put(scopeKey, parsedPresets);

		_report(scopeKey, parsedPresets._presetDefinitions);

		return parsedPresets._presetDefinitions;
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
					ImageTransformationConfigurationValidator.validate(
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
					ImageTransformationConfigurationValidator.getMarkupShape(
						presetDefinition)));
		}
	}

	/**
	 * Orders a preset's breakpoints by the order breakpoints are declared
	 * rather than by the order the preset's own keys appear, so ordering is
	 * decided once for the whole installation. The unconditional breakpoint
	 * always sorts last, being the catch all.
	 */
	private List<BreakpointDefinition> _toBreakpointDefinitions(
		Map<String, String> breakpoints,
		Map<String, Map<String, String>> breakpointProperties) {

		List<BreakpointDefinition> breakpointDefinitions = new ArrayList<>(
			breakpointProperties.size());

		for (Map.Entry<String, String> entry : breakpoints.entrySet()) {
			Map<String, String> properties = breakpointProperties.get(
				entry.getKey());

			if (properties == null) {
				continue;
			}

			breakpointDefinitions.add(
				new BreakpointDefinition(
					GetterUtil.getBoolean(properties.get(_AUTO_SIZES)),
					entry.getKey(), _toMaxWidth(properties.get(_MAX_WIDTH)),
					entry.getValue(), properties.get(_SIZES),
					_toTransformations(properties.get(_TRANSFORMATIONS))));
		}

		Map<String, String> properties = breakpointProperties.get(
			_NAME_DEFAULT);

		if (properties != null) {
			breakpointDefinitions.add(
				new BreakpointDefinition(
					GetterUtil.getBoolean(properties.get(_AUTO_SIZES)),
					_NAME_DEFAULT, _toMaxWidth(properties.get(_MAX_WIDTH)),
					null, properties.get(_SIZES),
					_toTransformations(properties.get(_TRANSFORMATIONS))));
		}

		return breakpointDefinitions;
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
	private Map<String, String> _toBreakpoints(String[] breakpoints) {
		Map<String, String> mediaQueries = new LinkedHashMap<>();

		if (breakpoints == null) {
			return mediaQueries;
		}

		for (String breakpoint : breakpoints) {
			if (Validator.isBlank(breakpoint)) {
				continue;
			}

			int i = breakpoint.indexOf(StringPool.EQUAL);

			if (i <= 0) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Ignoring malformed breakpoint entry " + breakpoint);
				}

				continue;
			}

			String key = StringUtil.trim(breakpoint.substring(0, i));

			String[] keyParts = StringUtil.split(key, StringPool.PERIOD);

			if ((keyParts.length != 2) || !_MEDIA.equals(keyParts[1])) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring unrecognized breakpoint key " + key);
				}

				continue;
			}

			mediaQueries.put(
				keyParts[0], StringUtil.trim(breakpoint.substring(i + 1)));
		}

		return mediaQueries;
	}

	private Integer _toMaxWidth(String value) {
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

	private Map<String, PresetDefinition> _toPresetDefinitions(
		Map<String, String> breakpoints, String[] presets) {

		if (presets == null) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new LinkedHashMap<>();
		Map<String, Boolean> lazyValues = new LinkedHashMap<>();
		Map<String, Map<String, Map<String, String>>> presetProperties =
			new LinkedHashMap<>();

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

			if (!_NAME_DEFAULT.equals(keyParts[1]) &&
				!breakpoints.containsKey(keyParts[1])) {

				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Ignoring preset key ", key,
							" because no breakpoint named ", keyParts[1],
							" is declared"));
				}

				continue;
			}

			Map<String, Map<String, String>> breakpointProperties =
				presetProperties.computeIfAbsent(
					keyParts[0], presetName -> new LinkedHashMap<>());

			Map<String, String> properties =
				breakpointProperties.computeIfAbsent(
					keyParts[1], breakpointName -> new LinkedHashMap<>());

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
					_toBreakpointDefinitions(breakpoints, entry.getValue())));
		}

		return presetDefinitions;
	}

	private Map<String, String> _toTransformations(String value) {
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

	private static final String _AUTO_SIZES = "autoSizes";

	private static final PresetDefinition _FALLBACK = new PresetDefinition(
		null, null, "default",
		Collections.singletonList(
			new BreakpointDefinition(
				false, "default", null, null, "100vw",
				Collections.<String, String>emptyMap())));

	private static final String _LABEL = "label";

	private static final String _LAZY = "lazy";

	private static final String _MAX_WIDTH = "maxWidth";

	private static final String _MEDIA = "media";

	private static final String _NAME_DEFAULT = "default";

	private static final String _SIZES = "sizes";

	private static final String _TRANSFORMATIONS = "transformations";

	private static final Log _log = LogFactoryUtil.getLog(
		PresetDefinitionResolver.class);

	private final ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper;
	private final Map<Long, ParsedPresets> _parsedPresets =
		new ConcurrentHashMap<>();

	private static class ParsedPresets {

		private ParsedPresets(
			int contentHash, Map<String, PresetDefinition> presetDefinitions) {

			_contentHash = contentHash;
			_presetDefinitions = presetDefinitions;
		}

		private final int _contentHash;
		private final Map<String, PresetDefinition> _presetDefinitions;

	}

}