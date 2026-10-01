/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.fastly;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpComponentsUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.responsive.image.ResponsiveImageURLTransformer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;

/**
 * Applies image transformations as Fastly Image Optimizer URL parameters.
 *
 * <p>
 * To minimize the number of CDN cache entries, image transformations not
 * supported by Fastly Image Optimizer are ignored. Also, URL parameters are
 * sorted in order to produce exactly the same URL regardless of the insertion
 * order in the image transformations map.
 * </p>
 *
 * <p>
 * An image transformation replaces the parameter of the same name the URL
 * already carries, so transforming the same image twice always produces the
 * same URL. Every other parameter is left untouched, and the parameters are
 * appended before any anchor.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ResponsiveImageURLTransformer.class)
public class FastlyImageOptimizerURLTransformer
	implements ResponsiveImageURLTransformer {

	public static final String NAME = "fastlyImageOptimizer";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String transform(
		String url, Map<String, String> imageTransformations) {

		if (Validator.isBlank(url) || MapUtil.isEmpty(imageTransformations)) {
			return url;
		}

		Map<String, String> parameters = new TreeMap<>();

		for (Map.Entry<String, String> entry :
				imageTransformations.entrySet()) {

			if (Validator.isBlank(entry.getKey()) ||
				Validator.isBlank(entry.getValue())) {

				continue;
			}

			if (!_supportedParameterNames.contains(entry.getKey())) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Ignoring unknown image transformation " +
							entry.getKey());
				}

				continue;
			}

			parameters.put(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			url = HttpComponentsUtil.setParameter(
				url, entry.getKey(), entry.getValue());
		}

		return url;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FastlyImageOptimizerURLTransformer.class);

	/**
	 * @see <a href="https://www.fastly.com/documentation/reference/io">
	 *      Fastly Image Optimizer reference</a>
	 */
	private static final Set<String> _supportedParameterNames = new HashSet<>(
		Arrays.asList(
			"auto", "bg-color", "blur", "brightness", "bw", "canvas",
			"contrast", "crop", "disable", "dpr", "enable", "fit", "format",
			"frame", "height", "level", "metadata", "optimize", "orient", "pad",
			"precrop", "profile", "quality", "resize-filter", "saturation",
			"sharpen", "trim", "viewbox", "width"));

}