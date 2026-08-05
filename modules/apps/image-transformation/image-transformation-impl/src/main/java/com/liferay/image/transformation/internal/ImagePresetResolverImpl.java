/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetGroup;
import com.liferay.image.transformation.ImagePresetResolver;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Parses the flat preset configuration into {@link ImagePresetGroup} objects.
 *
 * <p>
 * Flat because OSGi configuration files are flat: they hold typed key value
 * pairs and arrays of strings, with no nesting. The breakpoint segment in a
 * preset key is what lets one group describe several media conditions without a
 * structured format.
 * </p>
 *
 * <p>
 * Breakpoints are parsed separately and referenced by name, so a media
 * condition is written once for the whole installation rather than repeated in
 * every group that uses it.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ImagePresetResolver.class)
public class ImagePresetResolverImpl implements ImagePresetResolver {

	@Override
	public ImagePresetGroup resolve(long companyId, String presetGroupName) {
		if (Validator.isBlank(presetGroupName)) {
			presetGroupName = _NAME_DEFAULT;
		}

		ImagePresetGroup imagePresetGroup = _getImagePresetGroups(
			companyId
		).get(
			presetGroupName
		);

		if (imagePresetGroup != null) {
			return imagePresetGroup;
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"No preset group named " + presetGroupName +
					", using fallback");
		}

		return _FALLBACK;
	}

	/**
	 * Returns the parsed groups for a company, reparsing only when the
	 * underlying configuration has actually changed.
	 *
	 * <p>
	 * Memoized on the configuration's own contents rather than invalidated by
	 * an event: a stale cache would silently serve the previous layout after an
	 * administrator edited it, and there is no reliable notification to hang
	 * invalidation on.
	 * </p>
	 */
	private Map<String, ImagePresetGroup> _getImagePresetGroups(
		long companyId) {

		ImageTransformationConfiguration imageTransformationConfiguration =
			_imageTransformationConfigurationHelper.
				getImageTransformationConfiguration(companyId);

		if (imageTransformationConfiguration == null) {
			return Collections.emptyMap();
		}

		String[] breakpoints = imageTransformationConfiguration.breakpoints();
		String[] presets = imageTransformationConfiguration.presets();

		int contentHash =
			(31 * Arrays.hashCode(breakpoints)) + Arrays.hashCode(presets);

		ParsedPresets parsedPresets = _parsedPresets.get(companyId);

		if ((parsedPresets != null) &&
			(parsedPresets._contentHash == contentHash)) {

			return parsedPresets._imagePresetGroups;
		}

		parsedPresets = new ParsedPresets(
			contentHash,
			_toImagePresetGroups(_toBreakpoints(breakpoints), presets));

		_parsedPresets.put(companyId, parsedPresets);

		return parsedPresets._imagePresetGroups;
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

	private Map<String, ImagePresetGroup> _toImagePresetGroups(
		Map<String, String> breakpoints, String[] presets) {

		if (presets == null) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new LinkedHashMap<>();
		Map<String, Map<String, Map<String, String>>> groups =
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

			Map<String, Map<String, String>> group = groups.computeIfAbsent(
				keyParts[0], groupName -> new LinkedHashMap<>());

			Map<String, String> breakpointPreset = group.computeIfAbsent(
				keyParts[1], breakpointName -> new LinkedHashMap<>());

			breakpointPreset.put(keyParts[2], value);
		}

		Map<String, ImagePresetGroup> imagePresetGroups = new LinkedHashMap<>();

		for (Map.Entry<String, Map<String, Map<String, String>>> entry :
				groups.entrySet()) {

			imagePresetGroups.put(
				entry.getKey(),
				new ImagePresetGroup(
					labels.get(entry.getKey()), entry.getKey(),
					_toImagePresets(breakpoints, entry.getValue())));
		}

		return imagePresetGroups;
	}

	/**
	 * Orders presets by breakpoint declaration order rather than by the order a
	 * group's own keys appear, so ordering is decided once for the whole
	 * installation. The unconditional preset always sorts last, being the catch
	 * all.
	 */
	private List<ImagePreset> _toImagePresets(
		Map<String, String> breakpoints,
		Map<String, Map<String, String>> group) {

		List<ImagePreset> imagePresets = new ArrayList<>(group.size());

		for (Map.Entry<String, String> entry : breakpoints.entrySet()) {
			Map<String, String> breakpointPreset = group.get(entry.getKey());

			if (breakpointPreset == null) {
				continue;
			}

			imagePresets.add(
				new ImagePreset(
					entry.getKey(),
					_toMaxWidth(breakpointPreset.get(_MAX_WIDTH)),
					entry.getValue(), breakpointPreset.get(_SIZES),
					_toTransformations(
						breakpointPreset.get(_TRANSFORMATIONS))));
		}

		Map<String, String> breakpointPreset = group.get(_NAME_DEFAULT);

		if (breakpointPreset != null) {
			imagePresets.add(
				new ImagePreset(
					_NAME_DEFAULT,
					_toMaxWidth(breakpointPreset.get(_MAX_WIDTH)), null,
					breakpointPreset.get(_SIZES),
					_toTransformations(
						breakpointPreset.get(_TRANSFORMATIONS))));
		}

		return imagePresets;
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

	private static final ImagePresetGroup _FALLBACK = new ImagePresetGroup(
		null, "default",
		Collections.singletonList(
			new ImagePreset(
				"default", null, null, "100vw",
				Collections.<String, String>emptyMap())));

	private static final String _LABEL = "label";

	private static final String _MAX_WIDTH = "maxWidth";

	private static final String _MEDIA = "media";

	private static final String _NAME_DEFAULT = "default";

	private static final String _SIZES = "sizes";

	private static final String _TRANSFORMATIONS = "transformations";

	private static final Log _log = LogFactoryUtil.getLog(
		ImagePresetResolverImpl.class);

	@Reference
	private ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper;

	private final Map<Long, ParsedPresets> _parsedPresets =
		new ConcurrentHashMap<>();

	private static class ParsedPresets {

		private ParsedPresets(
			int contentHash, Map<String, ImagePresetGroup> imagePresetGroups) {

			_contentHash = contentHash;
			_imagePresetGroups = imagePresetGroups;
		}

		private final int _contentHash;
		private final Map<String, ImagePresetGroup> _imagePresetGroups;

	}

}