/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetBreakpoint;
import com.liferay.image.transformation.ImagePresetResolver;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationValidator;
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Parses the flat preset configuration into {@link ImagePreset} objects.
 *
 * <p>
 * Flat because OSGi configuration files are flat: they hold typed key value
 * pairs and arrays of strings, with no nesting. The breakpoint segment in a
 * preset key is what lets one preset describe several media conditions without a
 * structured format.
 * </p>
 *
 * <p>
 * Breakpoints are parsed separately and referenced by name, so a media
 * condition is written once for the whole installation rather than repeated in
 * every preset that uses it.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ImagePresetResolver.class)
public class ImagePresetResolverImpl implements ImagePresetResolver {

	@Override
	public ImagePreset resolve(long companyId, String presetName) {
		if (Validator.isBlank(presetName)) {
			presetName = _NAME_DEFAULT;
		}

		ImagePreset imagePreset = _getImagePresets(
			companyId
		).get(
			presetName
		);

		if (imagePreset != null) {
			return imagePreset;
		}

		if (_log.isDebugEnabled()) {
			_log.debug("No preset named " + presetName + ", using fallback");
		}

		return _FALLBACK;
	}

	@Activate
	protected void activate() {
		_imageTransformationConfigurationHelper =
			ImageTransformationFactory.
				createImageTransformationConfigurationHelper(
					_configurationProvider, _portal);
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
	private Map<String, ImagePreset> _getImagePresets(long companyId) {
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

			return parsedPresets._imagePresets;
		}

		parsedPresets = new ParsedPresets(
			contentHash, _toImagePresets(_toBreakpoints(breakpoints), presets));

		_parsedPresets.put(companyId, parsedPresets);

		_report(companyId, parsedPresets._imagePresets);

		return parsedPresets._imagePresets;
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
		long companyId, Map<String, ImagePreset> imagePresets) {

		if (_log.isWarnEnabled()) {
			for (String problem :
					ImageTransformationConfigurationValidator.validate(
						imagePresets)) {

				_log.warn(problem);
			}
		}

		if (!_log.isDebugEnabled()) {
			return;
		}

		for (ImagePreset imagePreset : imagePresets.values()) {
			_log.debug(
				StringBundler.concat(
					"Preset ", imagePreset.getName(), " of company ", companyId,
					" renders ",
					ImageTransformationConfigurationValidator.getMarkupShape(
						imagePreset)));
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

	/**
	 * Orders presets by breakpoint declaration order rather than by the order a
	 * preset's own keys appear, so ordering is decided once for the whole
	 * installation. The unconditional preset always sorts last, being the catch
	 * all.
	 */
	private List<ImagePresetBreakpoint> _toImagePresetBreakpoints(
		Map<String, String> breakpoints,
		Map<String, Map<String, String>> breakpointProperties) {

		List<ImagePresetBreakpoint> imagePresetBreakpoints = new ArrayList<>(
			breakpointProperties.size());

		for (Map.Entry<String, String> entry : breakpoints.entrySet()) {
			Map<String, String> properties = breakpointProperties.get(
				entry.getKey());

			if (properties == null) {
				continue;
			}

			imagePresetBreakpoints.add(
				new ImagePresetBreakpoint(
					GetterUtil.getBoolean(properties.get(_AUTO_SIZES)),
					entry.getKey(), _toMaxWidth(properties.get(_MAX_WIDTH)),
					entry.getValue(), properties.get(_SIZES),
					_toTransformations(properties.get(_TRANSFORMATIONS))));
		}

		Map<String, String> properties = breakpointProperties.get(
			_NAME_DEFAULT);

		if (properties != null) {
			imagePresetBreakpoints.add(
				new ImagePresetBreakpoint(
					GetterUtil.getBoolean(properties.get(_AUTO_SIZES)),
					_NAME_DEFAULT, _toMaxWidth(properties.get(_MAX_WIDTH)),
					null, properties.get(_SIZES),
					_toTransformations(properties.get(_TRANSFORMATIONS))));
		}

		return imagePresetBreakpoints;
	}

	private Map<String, ImagePreset> _toImagePresets(
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

		Map<String, ImagePreset> imagePresets = new LinkedHashMap<>();

		for (Map.Entry<String, Map<String, Map<String, String>>> entry :
				presetProperties.entrySet()) {

			imagePresets.put(
				entry.getKey(),
				new ImagePreset(
					labels.get(entry.getKey()), lazyValues.get(entry.getKey()),
					entry.getKey(),
					_toImagePresetBreakpoints(breakpoints, entry.getValue())));
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

	private static final String _AUTO_SIZES = "autoSizes";

	private static final ImagePreset _FALLBACK = new ImagePreset(
		null, null, "default",
		Collections.singletonList(
			new ImagePresetBreakpoint(
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
		ImagePresetResolverImpl.class);

	@Reference
	private ConfigurationProvider _configurationProvider;

	private ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper;
	private final Map<Long, ParsedPresets> _parsedPresets =
		new ConcurrentHashMap<>();

	@Reference
	private Portal _portal;

	private static class ParsedPresets {

		private ParsedPresets(
			int contentHash, Map<String, ImagePreset> imagePresets) {

			_contentHash = contentHash;
			_imagePresets = imagePresets;
		}

		private final int _contentHash;
		private final Map<String, ImagePreset> _imagePresets;

	}

}