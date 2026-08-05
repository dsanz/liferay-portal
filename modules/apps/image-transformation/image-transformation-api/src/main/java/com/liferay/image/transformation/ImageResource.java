/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

/**
 * The source image to be transformed, independently of where it is stored.
 *
 * <p>
 * Deliberately not tied to {@code FileEntry}: transformable images also live in
 * OSGi modules, in the legacy portal image path, or on a completely different
 * host. Providers that need more than this interface exposes narrow the type
 * (see {@link FileEntryImageResource}) and use {@link
 * com.liferay.image.transformation.spi.ImageTransformationProvider#isTransformable}
 * to decline resources they cannot serve.
 * </p>
 *
 * <p>
 * This is the <b>input</b> to a transformation; the outputs are {@link
 * ImageVariant} instances.
 * </p>
 *
 * <p>
 * Deliberately carries nothing about cache freshness either. How long a
 * transformed image may be held is decided by the <code>Cache-Control</code>
 * the origin returns and by the CDN's own invalidation, both of which are
 * configured elsewhere. Encoding a version into the URL here would be cache
 * policy smuggled in as a transformation parameter.
 * </p>
 *
 * <p>
 * Deliberately carries no intrinsic dimensions. They are obtainable for a
 * document library file, but only through raw metadata at a cost of several
 * queries per image, and they buy less than they appear to: with upscaling
 * disabled, a variant wider than the original simply returns the original, so
 * truncating a ladder at the source width mainly forfeits resolution the
 * browser could have used. Bound ladders with {@link ImagePreset#getMaxWidth()}
 * instead, which describes the rendered size and costs nothing to read.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageResource {

	/**
	 * Returns the image's mime type, or <code>null</code> if it is not known.
	 *
	 * <p>
	 * Providers use it to decline formats that must not be resampled, such as
	 * SVG, where transforming would rasterize a vector and lose the point of
	 * shipping it.
	 * </p>
	 *
	 * <p>
	 * Frequently <code>null</code> outside the document library, which always
	 * stores it. A caller pointing at a module resource or a foreign host may
	 * simply not know, and providers are expected to decline rather than guess:
	 * an untransformed image is a worse outcome than a transformed one, but a
	 * rasterized logo is worse than both.
	 * </p>
	 *
	 * @return the mime type, or <code>null</code>
	 */
	public String getMimeType();

	/**
	 * Returns the URL the image is served from today, before any
	 * transformation. May be portal relative or absolute.
	 *
	 * @return the untransformed URL
	 */
	public String getURL();

}