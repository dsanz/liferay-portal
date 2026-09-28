/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

/**
 * @author Daniel Sanz
 */
public class ResponsiveImageCandidateBuilder {

	public static ResponsiveImageCandidateBuilder url(String url) {
		return new ResponsiveImageCandidateBuilder(url);
	}

	public ResponsiveImageCandidate build() {
		return new ResponsiveImageCandidate(_url, _width);
	}

	public ResponsiveImageCandidateBuilder width(Integer width) {
		_width = width;

		return this;
	}

	private ResponsiveImageCandidateBuilder(String url) {
		_url = url;
	}

	private final String _url;
	private Integer _width;

}