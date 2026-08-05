/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.fastly.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;

/**
 * Renders transformations as Fastly Image Optimizer URL parameters.
 *
 * <p>
 * The whole of Fastly's URL vocabulary lives here. Nothing above this class
 * knows that Fastly spells a background color <code>bg-color</code>, which is
 * what makes adding another provider a matter of writing a sibling of this
 * class rather than touching the framework.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(
	property = ImageTransformationURLRenderer.RENDERER_NAME + "=" + FastlyImageTransformationURLRenderer.NAME,
	service = ImageTransformationURLRenderer.class
)
public class FastlyImageTransformationURLRenderer
	implements ImageTransformationURLRenderer {

	public static final String NAME = "fastly";

	@Override
	public String render(String url, Map<String, String> transformations) {
		if (Validator.isBlank(url) || (transformations == null) ||
			transformations.isEmpty()) {

			return url;
		}

		// Sorted so equal transformations always render byte identically. Two
		// URLs differing only in parameter order are distinct cache objects
		// holding the same image, which quietly halves the hit ratio.

		Map<String, String> parameters = new TreeMap<>();

		for (Map.Entry<String, String> entry : transformations.entrySet()) {
			String name = entry.getKey();

			if (Validator.isBlank(name) ||
				Validator.isBlank(entry.getValue())) {

				continue;
			}

			parameters.put(
				_parameterNames.getOrDefault(name, name), entry.getValue());
		}

		if (parameters.isEmpty()) {
			return url;
		}

		StringBundler sb = new StringBundler((parameters.size() * 4) + 1);

		sb.append(url);

		boolean first = !url.contains(StringPool.QUESTION);

		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			if (first) {
				sb.append(StringPool.QUESTION);

				first = false;
			}
			else {
				sb.append(StringPool.AMPERSAND);
			}

			sb.append(entry.getKey());
			sb.append(StringPool.EQUAL);
			sb.append(_encode(entry.getValue()));
		}

		return sb.toString();
	}

	private String _encode(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
		}
		catch (UnsupportedEncodingException unsupportedEncodingException) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to encode " + value, unsupportedEncodingException);
			}

			return value;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FastlyImageTransformationURLRenderer.class);

	/**
	 * Transformation names that Fastly spells differently. Anything absent is
	 * passed through unchanged, so callers can use parameters this framework
	 * never enumerated.
	 *
	 * @see <a href="https://www.fastly.com/documentation/reference/io/">Fastly
	 *      Image Optimizer reference</a>
	 */
	private static final Map<String, String> _parameterNames =
		HashMapBuilder.put(
			"bgColor", "bg-color"
		).put(
			"resizeFilter", "resize-filter"
		).put(
			"trimColor", "trim-color"
		).build();

}