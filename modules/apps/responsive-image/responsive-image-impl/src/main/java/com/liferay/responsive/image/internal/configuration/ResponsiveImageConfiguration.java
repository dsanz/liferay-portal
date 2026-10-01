/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Daniel Sanz
 */
@ExtendedObjectClassDefinition(
	category = "adaptive-media",
	scope = ExtendedObjectClassDefinition.Scope.GROUP
)
@Meta.OCD(
	id = "com.liferay.responsive.image.internal.configuration.ResponsiveImageConfiguration",
	localization = "content/Language",
	name = "responsive-image-configuration-name"
)
public interface ResponsiveImageConfiguration {

	/**
	 * The widths available to generate, in pixels.
	 *
	 * <p>
	 * Two costs pull in opposite directions: every extra width is another
	 * distinct CDN cache object, while wider gaps mean shipping more pixels
	 * than needed. The default is a roughly 1.4x ladder, which caps overshoot
	 * near 2x the pixel count.
	 * </p>
	 */
	@Meta.AD(
		deflt = "320|480|640|960|1280|1920|2560", name = "candidate-widths",
		required = false
	)
	public String[] candidateWidths();

	/**
	 * Host the transformed image URLs are served from, for example
	 * <code>https://images.example.com</code>, so that images can be delivered
	 * by a service that fronts nothing else. Leave it blank to use the CDN host
	 * configured for the instance.
	 */
	@Meta.AD(deflt = "", name = "cdn-host", required = false)
	public String cdnHost();

	/**
	 * Transformations applied to every generated rendition, as
	 * <code>name=value</code> entries.
	 */
	@Meta.AD(
		deflt = "disable=upscale", name = "default-transformations",
		required = false
	)
	public String[] defaultTransformations();

	/**
	 * The media conditions available to presets, typically a theme's
	 * breakpoints, as <code>&lt;name&gt;.media=&lt;media condition&gt;</code>
	 * entries.
	 *
	 * <pre>
	 * narrow.media=(max-width: 767px)
	 * wide.media=(min-width: 768px)
	 * </pre>
	 */
	@Meta.AD(deflt = "", name = "media-conditions", required = false)
	public String[] mediaConditions();

	/**
	 * Presets, as flat <code>key=value</code> entries in one of two
	 * forms:
	 *
	 * <p>
	 * <code>&lt;preset&gt;.label</code> names the preset for authoring UIs and
	 * <code>&lt;preset&gt;.lazy</code> says whether images in this placement are
	 * lazily loaded by default.
	 * <code>&lt;preset&gt;.&lt;media condition&gt;.sizes</code>,
	 * <code>.transformations</code>, <code>.maxWidth</code> and
	 * <code>.autoSizes</code> describe what to generate under one media
	 * condition, which must be one declared in {@link #mediaConditions()}.
	 * </p>
	 *
	 * <pre>
	 * hero.label=Hero
	 * hero.narrow.sizes=100vw
	 * hero.narrow.transformations=crop=1:1
	 * hero.wide.sizes=100vw
	 * hero.wide.transformations=crop=16:9
	 * card.label=Card
	 * card.default.sizes=(min-width: 992px) 25vw, 100vw
	 * thumb.label=Thumbnail
	 * thumb.default.sizes=96px
	 * thumb.default.maxWidth=320
	 * card.lazy=true
	 * card.default.autoSizes=true
	 * </pre>
	 */
	@Meta.AD(deflt = "", name = "presets", required = false)
	public String[] presets();

	/**
	 * Name of the image optimization service whose URL vocabulary is used,
	 * for example <code>fastly</code>. Must match the name reported by a
	 * deployed {@code ResponsiveImageURLTransformer}.
	 */
	@Meta.AD(
		deflt = "", name = "responsive-image-url-transformer-name",
		required = false
	)
	public String responsiveImageURLTransformerName();

}