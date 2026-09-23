/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.internal.util.URLUtil;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Daniel Sanz
 */
public class TransformedImageAbsolutePortalURLBuilderImpl
	implements TransformedImageAbsolutePortalURLBuilder {

	public static final String TRANSFORMATION_FORMAT = "format";

	public static final String TRANSFORMATION_HEIGHT = "height";

	public static final String TRANSFORMATION_QUALITY = "quality";

	public static final String TRANSFORMATION_WIDTH = "width";

	public TransformedImageAbsolutePortalURLBuilderImpl(
		String cdnHost, String pathProxy, String imagePath) {

		_cdnHost = cdnHost;
		_pathProxy = pathProxy;
		_imagePath = imagePath;

		_ignoreCDNHost = false;
	}

	@Override
	public String build() {
		StringBundler sb = new StringBundler();

		URLUtil.appendURL(
			sb, _cdnHost, _ignoreCDNHost, StringPool.BLANK, _pathProxy,
			_imagePath);

		String url = sb.toString();

		if (_transformations.isEmpty() ||
			(_imageTransformationURLRenderer == null)) {

			return url;
		}

		return _imageTransformationURLRenderer.render(url, _transformations);
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder format(String format) {
		return param(TRANSFORMATION_FORMAT, format);
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder height(int height) {
		return param(TRANSFORMATION_HEIGHT, String.valueOf(height));
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder ignoreCDNHost() {
		_ignoreCDNHost = true;

		return this;
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder param(
		String name, String value) {

		if ((name == null) || (value == null)) {
			return this;
		}

		_transformations.put(name, value);

		return this;
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder quality(int quality) {
		return param(TRANSFORMATION_QUALITY, String.valueOf(quality));
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder renderer(
		ImageTransformationURLRenderer imageTransformationURLRenderer) {

		_imageTransformationURLRenderer = imageTransformationURLRenderer;

		return this;
	}

	@Override
	public TransformedImageAbsolutePortalURLBuilder width(int width) {
		return param(TRANSFORMATION_WIDTH, String.valueOf(width));
	}

	private final String _cdnHost;
	private boolean _ignoreCDNHost;
	private final String _imagePath;
	private ImageTransformationURLRenderer _imageTransformationURLRenderer;
	private final String _pathProxy;

	/**
	 * Sorted so that equal transformations always render to a byte identical
	 * URL. Two URLs that differ only in parameter order are distinct cache
	 * objects for an identical image.
	 */
	private final Map<String, String> _transformations = new TreeMap<>();

}