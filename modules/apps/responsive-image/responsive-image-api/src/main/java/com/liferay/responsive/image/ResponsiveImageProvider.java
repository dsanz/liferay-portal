/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * Produces the renditions of an image, as a model for a renderer to present.
 *
 * <p>
 * Register an implementation as an OSGi service to add a way of generating
 * renditions. Which one is used is decided by configuration, by name, rather than
 * by service ranking: ranking is workable with two implementations and becomes
 * opaque once several are deployed at once, and the choice is per company.
 * </p>
 *
 * <p>
 * Markup is deliberately not produced here. Rendering is not the only consumer
 * of the model: the page editor's rendition picker presents it as JSON, and
 * its adaptive media processor patches URLs onto existing DOM elements.
 * Neither wants a tag, and one renderer over one model is what stops two
 * consumers disagreeing about the renditions.
 * </p>
 *
 * <p>
 * Implemented here, not called here. Consumers call {@link
 * com.liferay.responsive.image.ResponsiveImageMarkupRenderer#render},
 * which selects the configured provider and answers
 * <code>null</code> when none produced anything. An implementation of this
 * interface is therefore free to return nothing when it does not support a
 * resource, and should not synthesize a fallback of its own.
 * </p>
 *
 * <p>
 * Deliberately not <code>@ProviderType</code>. The two vocabularies invert the
 * word: in <em>service provider interface</em> the provider is the third party
 * plugging in, while <code>@ProviderType</code> marks a type only this API's
 * own implementation provides. Anyone adding a CDN implements this, so consumer
 * semantics is correct and adding a method here is a breaking change.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ResponsiveImageProvider {

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
	 * {@link #isSupported(ImageResource)} returned <code>true</code>.
	 *
	 * <p>
	 * Returning the whole answer rather than only the sources puts the fallback
	 * <code>src</code> in the hands of whoever knows what a good one is. A
	 * provider generating a ladder on demand can point it at a middle rendition
	 * instead of the original URL, which may be far larger than
	 * anything a browser ignoring <code>srcset</code> should be handed.
	 * </p>
	 *
	 * @param  responsiveImageRequest what the caller wants
	 * @return the resolved image, possibly with no sources
	 * @throws PortalException if the resource could not be read
	 */
	public ResponsiveImage getResponsiveImage(
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException;

	/**
	 * Returns <code>true</code> if this provider supports the given image.
	 *
	 * <p>
	 * Expected to return <code>false</code> generously. A provider backed by a
	 * CDN supports only images that the CDN actually serves, so an image
	 * on a third party host is out of reach no matter what parameters are
	 * appended to it. Formats that must not be resampled, such as SVG, should
	 * be declined here too.
	 * </p>
	 *
	 * @param  imageResource the image to test
	 * @return <code>true</code> if this provider supports the image
	 */
	public boolean isSupported(ImageResource imageResource);

}