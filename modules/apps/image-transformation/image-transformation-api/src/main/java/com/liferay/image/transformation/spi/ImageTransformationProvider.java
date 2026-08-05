/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.spi;

import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.portal.kernel.exception.PortalException;

/**
 * Produces renditions of an image, and the markup that presents them.
 *
 * <p>
 * Register an implementation as an OSGi service to add a source of transformed
 * images. Which one is used is decided by configuration, by name, rather than
 * by service ranking: ranking is workable with two implementations and becomes
 * opaque once several are deployed at once, and the choice is per company.
 * </p>
 *
 * <p>
 * Both producing the model and rendering it live here, on one interface,
 * because they must agree. Markup shape follows from how renditions were
 * produced: a provider limited to a fixed set of pregenerated renditions needs
 * <code>&lt;source media&gt;</code>, while one that generates any width on
 * demand wants <code>srcset</code> and lets the browser choose. Splitting them
 * across two registrations would allow a company to end up with one provider's
 * renditions presented by another's markup.
 * </p>
 *
 * <p>
 * Both are still needed. Rendering is not the only consumer of the model: the
 * page editor's rendition picker presents it as JSON, and its adaptive media
 * processor patches URLs onto existing DOM elements. Neither wants a tag.
 * </p>
 *
 * <p>
 * Providers that honor presets obtain them from {@link ImagePresetResolver};
 * presets are framework owned. A provider that cannot honor one, because its
 * renditions were generated in advance, determines its own groups instead.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageTransformationProvider {

	/**
	 * Returns the name identifying this provider (for example
	 * <code>adaptive-media</code> or <code>cdn</code>).
	 *
	 * @return the provider name
	 */
	public String getName();

	/**
	 * Returns the renditions of the given image, grouped by the media condition
	 * they apply under, together with the URL to fall back to. Only called when
	 * {@link #isTransformable(ImageResource)} returned <code>true</code>.
	 *
	 * <p>
	 * Returning the whole answer rather than only the groups puts the fallback
	 * <code>src</code> in the hands of whoever knows what a good one is. A
	 * provider generating a ladder on demand can point it at a middle rendition
	 * instead of the untransformed original, which may be far larger than
	 * anything a browser ignoring <code>srcset</code> should be handed.
	 * </p>
	 *
	 * @param  responsiveImageRequest what the caller wants
	 * @return the resolved image, possibly with no variant groups
	 * @throws PortalException if the resource could not be read
	 */
	public ResponsiveImage getResponsiveImage(
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException;

	/**
	 * Returns <code>true</code> if this provider can transform the given
	 * image.
	 *
	 * <p>
	 * Expected to return <code>false</code> generously. A provider backed by a
	 * CDN can only transform images that the CDN actually serves, so an image
	 * on a third party host is out of reach no matter what parameters are
	 * appended to it. Formats that must not be resampled, such as SVG, should
	 * be declined here too.
	 * </p>
	 *
	 * @param  imageResource the image to test
	 * @return <code>true</code> if this provider can transform the image
	 */
	public boolean isTransformable(ImageResource imageResource);

	/**
	 * Returns markup for the given image.
	 *
	 * <p>
	 * Defaulted so a provider contributing only to model consumers need not
	 * implement it; its callers get the original tag back unchanged.
	 * </p>
	 *
	 * <p>
	 * An implementation that builds its own markup should call {@link
	 * #getResponsiveImage} rather than resolving renditions a second way, so
	 * that what is rendered and what model consumers see cannot diverge. An
	 * implementation that delegates to a provider's existing renderer is
	 * trusted to be self consistent, which is the price of not rewriting it.
	 * </p>
	 *
	 * @param  originalImgTag the original image tag, whose attributes are
	 *         preserved
	 * @param  responsiveImageRequest what the caller wants
	 * @return the markup, or <code>originalImgTag</code> if nothing could be
	 *         rendered
	 * @throws PortalException if the resource could not be read
	 */
	public default String render(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		return originalImgTag;
	}

}