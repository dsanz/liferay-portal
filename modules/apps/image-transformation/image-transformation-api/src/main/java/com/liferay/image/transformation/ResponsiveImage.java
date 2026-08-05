/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import java.util.Collections;
import java.util.List;

/**
 * A resolved image, ready to be rendered or serialized.
 *
 * <p>
 * A model rather than a markup string on purpose. Content rewriting needs HTML,
 * but the page editor needs JSON and patches attributes onto elements that
 * already exist, and both should go through the same provider.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImage {

	/**
	 * Returns a result carrying only the untransformed URL, used whenever no
	 * provider claimed the resource so that callers always get something
	 * renderable.
	 *
	 * @param  src the untransformed URL
	 * @return the passthrough result
	 */
	public static ResponsiveImage passthrough(String src) {
		return new ResponsiveImage(
			Collections.<ImageVariantGroup>emptyList(), src);
	}

	public ResponsiveImage(List<ImageVariantGroup> variantGroups, String src) {
		_variantGroups = Collections.unmodifiableList(variantGroups);
		_src = src;
	}

	/**
	 * Returns the URL for the fallback <code>src</code> attribute. Always
	 * usable, whether or not any group was produced.
	 *
	 * @return the fallback URL
	 */
	public String getSrc() {
		return _src;
	}

	/**
	 * Returns the variant groups, or an empty list when no provider could
	 * transform the resource. Empty is not an error: callers render a plain
	 * image tag pointing at {@link #getSrc()}.
	 *
	 * <p>
	 * Order is significant. Source matching is first wins, so a group with a
	 * narrower media condition must precede a broader one.
	 * </p>
	 *
	 * @return the variant groups
	 */
	public List<ImageVariantGroup> getVariantGroups() {
		return _variantGroups;
	}

	private final String _src;
	private final List<ImageVariantGroup> _variantGroups;

}