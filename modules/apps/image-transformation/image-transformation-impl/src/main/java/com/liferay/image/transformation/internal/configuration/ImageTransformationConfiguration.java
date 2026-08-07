/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Daniel Sanz
 */
@ExtendedObjectClassDefinition(
	category = "adaptive-media",
	scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	id = "com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration",
	localization = "content/Language",
	name = "image-transformation-configuration-name"
)
public interface ImageTransformationConfiguration {

	/**
	 * Breakpoints available to presets, as
	 * <code>&lt;breakpoint&gt;.media=&lt;media condition&gt;</code> entries.
	 *
	 * <pre>
	 * narrow.media=(max-width: 767px)
	 * wide.media=(min-width: 768px)
	 * </pre>
	 *
	 * <p>
	 * Declared here once and referenced by name from {@link #presets()}, so a
	 * theme's grid lives in a single place instead of being repeated inside
	 * every preset group.
	 * </p>
	 *
	 * <p>
	 * <b>Order matters.</b> Presets render in the order their breakpoints are
	 * declared here, and browser source matching is first wins, so a narrower
	 * media condition must precede a broader one. The reserved name
	 * <code>default</code> means no media condition and always renders last, as
	 * the catch all.
	 * </p>
	 */
	@Meta.AD(deflt = "", name = "breakpoints", required = false)
	public String[] breakpoints();

	/**
	 * Transformations applied to every generated rendition, as
	 * <code>name=value</code> entries. The escape hatch for provider options
	 * this framework does not model (for example <code>optimize=medium</code>).
	 *
	 * <p>
	 * Defaults to disabling upscaling, which is what makes it safe not to bound
	 * ladders by the original's width: a variant wider than the source then
	 * returns the source instead of an enlarged copy of it, so the widest
	 * candidates cost no extra bytes when an author uploads a small image.
	 * </p>
	 */
	@Meta.AD(
		deflt = "disable=upscale", name = "default-transformations",
		required = false
	)
	public String[] defaultTransformations();

	/**
	 * Preset groups, as flat <code>key=value</code> entries in one of two
	 * forms:
	 *
	 * <p>
	 * <code>&lt;group&gt;.label</code> names the group for authoring UIs and
	 * <code>&lt;group&gt;.lazy</code> says whether images in this placement are
	 * lazily loaded by default.
	 * <code>&lt;group&gt;.&lt;breakpoint&gt;.sizes</code>,
	 * <code>.transformations</code>, <code>.maxWidth</code> and
	 * <code>.autoSizes</code> describe what to generate at one breakpoint,
	 * which must be one declared in {@link #breakpoints()}.
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
	 *
	 * <p>
	 * <code>lazy</code> is undeclared by default, which means eager. Loading
	 * eagerly costs bandwidth, while lazily loading the largest contentful
	 * image costs a Core Web Vital, so the default is the one that cannot
	 * regress it. A caller that knows where a particular image sits on the page
	 * overrides it per instance.
	 * </p>
	 *
	 * <p>
	 * <code>autoSizes</code> puts the <code>auto</code> keyword in front of
	 * that breakpoint's <code>sizes</code>, letting the browser measure the
	 * container instead of trusting a value that duplicates the theme's CSS.
	 * It applies only when the image is lazily loaded, because that is the only
	 * case a browser honors it, and <code>sizes</code> stays mandatory so that
	 * browsers without support still receive a real value.
	 * </p>
	 *
	 * <p>
	 * <code>maxWidth</code> stops a small placement from advertising the whole
	 * ladder: a 96 pixel thumbnail otherwise ships seven URLs for an image the
	 * browser will never choose. Set it to the rendered width multiplied by the
	 * highest pixel density worth serving. It is not derived from
	 * <code>sizes</code>, because only absolute lengths could be resolved
	 * server side and a wrong guess degrades image quality with no error.
	 * </p>
	 *
	 * <p>
	 * A group with one preset and no media condition renders as a plain
	 * <code>&lt;img&gt;</code>; several presets render as
	 * <code>&lt;picture&gt;</code>. Note that each additional breakpoint
	 * multiplies the number of distinct objects held at the edge by the number
	 * of variant widths.
	 * </p>
	 */
	@Meta.AD(
		deflt = "default.default.sizes=100vw", name = "presets",
		required = false
	)
	public String[] presets();

	/**
	 * Name of the active transformation provider (for example
	 * <code>cdn</code> or <code>adaptive-media</code>). Left blank, the
	 * framework falls back to Adaptive Media, preserving current behavior.
	 *
	 * <p>
	 * Selection is by name rather than by service ranking so that the active
	 * provider is greppable in configuration instead of implied by numbers
	 * spread across bundles.
	 * </p>
	 */
	@Meta.AD(deflt = "", name = "provider-name", required = false)
	public String providerName();

	/**
	 * Name of the image optimization provider whose URL vocabulary is used, for
	 * example <code>fastly</code>. Must match the
	 * <code>image.transformation.renderer.name</code> service property of a
	 * deployed renderer.
	 *
	 * <p>
	 * Separate from {@link #providerName()} because the two are orthogonal: the
	 * provider decides how renditions are produced, the renderer decides how
	 * their URLs are spelled. Adaptive Media is a provider with no renderer, and
	 * the generic CDN provider works with any of them.
	 * </p>
	 *
	 * <p>
	 * Left blank, nothing is transformed and images are served untouched, which
	 * is also what happens if it names a renderer that is not deployed.
	 * </p>
	 */
	@Meta.AD(deflt = "", name = "renderer-name", required = false)
	public String rendererName();

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
		deflt = "320|480|640|960|1280|1920|2560", name = "variant-widths",
		required = false
	)
	public String[] variantWidths();

}