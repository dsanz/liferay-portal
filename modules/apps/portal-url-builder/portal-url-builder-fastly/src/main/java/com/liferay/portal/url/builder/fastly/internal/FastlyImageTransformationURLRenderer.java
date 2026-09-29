/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder.fastly.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;

/**
 * Renders transformations as Fastly Image Optimizer URL parameters.
 *
 * <p>
 * To minimize number of CDN cache entries, transformations not recognized by
 * Fastly are ignored. Also, URL parameters are sorted in order to produce
 * exactly the same URL regardless of insertion order in transformations map.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ImageTransformationURLRenderer.class)
public class FastlyImageTransformationURLRenderer
	implements ImageTransformationURLRenderer {

	public static final String NAME = "fastly";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String render(String url, Map<String, String> transformations) {
		if (Validator.isBlank(url) || (transformations == null) ||
			transformations.isEmpty()) {

			return url;
		}

		Map<String, String> parameters = new TreeMap<>();

		for (Map.Entry<String, String> entry : transformations.entrySet()) {

			if (Validator.isBlank(entry.getKey()) ||
				Validator.isBlank(entry.getValue())) {

				continue;
			}

			if (!_parameterNames.contains(entry.getKey())) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring unknown transformation " + entry.getKey());
				}

				continue;
			}

			parameters.put(entry.getKey(), entry.getValue());
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
			return URLEncoder.encode(value, "UTF-8");
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
	 * Every parameter Fastly Image Optimizer defines.
	 *
	 * @see <a href="https://www.fastly.com/documentation/reference/io/">
	 *      Fastly Image Optimizer reference</a>
	 */
	private static final Set<String> _parameterNames = new HashSet<>(
		Arrays.asList(
			"auto", "bg-color", "blur", "brightness", "bw", "canvas",
			"contrast", "crop", "disable", "dpr", "enable", "fit", "format",
			"frame", "height", "level", "metadata", "optimize", "orient", "pad",
			"precrop", "profile", "quality", "resize-filter", "saturation",
			"sharpen", "trim", "viewbox", "width"));

}