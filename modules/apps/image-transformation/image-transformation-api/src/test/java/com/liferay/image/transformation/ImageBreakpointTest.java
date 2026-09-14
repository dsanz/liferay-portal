/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.image.transformation.preset.BreakpointPreset;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Sanz
 */
public class ImageBreakpointTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testAutoSizesIsIgnoredWhenNotLazy() {

		// A browser only honors sizes="auto" on a lazily loaded image, so
		// emitting it here would be an invalid attribute rather than a hint.

		ImageBreakpoint imageBreakpoint = ImageBreakpoint.from(
			_autoSizesPreset(), _variants, false);

		Assert.assertEquals("100vw", imageBreakpoint.getSizes());
	}

	@Test
	public void testAutoSizesKeepsDeclaredSizesAsFallback() {

		// The declared value stays behind the keyword so that a browser
		// without automatic sizing still receives a real one.

		ImageBreakpoint imageBreakpoint = ImageBreakpoint.from(
			_autoSizesPreset(), _variants, true);

		Assert.assertEquals("auto, 100vw", imageBreakpoint.getSizes());
	}

	@Test
	public void testFromCarriesPresetLayoutThrough() {

		// The single crossing point from configuration into output. A provider
		// that drifted from the preset it was asked to honor would show up
		// here first.

		BreakpointPreset breakpointPreset = new BreakpointPreset(
			false, "narrow", 320, "(max-width: 767px)", "100vw",
			HashMapBuilder.put(
				"crop", "1:1"
			).build());

		ImageBreakpoint imageBreakpoint = ImageBreakpoint.from(
			breakpointPreset, _variants, false);

		Assert.assertEquals(
			"(max-width: 767px)", imageBreakpoint.getMediaQuery());
		Assert.assertEquals("100vw", imageBreakpoint.getSizes());
		Assert.assertEquals(_variants, imageBreakpoint.getVariants());
	}

	@Test
	public void testOfAllowsAProviderToDetermineItsOwnGroup() {

		// Adaptive Media never sees a preset: its media conditions come from
		// its own configuration entries, and a breakpoint of one candidate
		// has no sizes to disambiguate.

		ImageBreakpoint imageBreakpoint = ImageBreakpoint.of(
			"(max-width: 640px)", null, _variants);

		Assert.assertEquals(
			"(max-width: 640px)", imageBreakpoint.getMediaQuery());
		Assert.assertNull(imageBreakpoint.getSizes());
	}

	@Test
	public void testUnconditionalGroupHasNoMediaQuery() {

		// One unconditional breakpoint is what renders as a plain img rather
		// than as a picture element.

		ImageBreakpoint imageBreakpoint = ImageBreakpoint.of(
			null, "100vw", Collections.<ImageBreakpointVariant>emptyList());

		Assert.assertNull(imageBreakpoint.getMediaQuery());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testVariantsAreUnmodifiable() {
		ImageBreakpoint imageBreakpoint = ImageBreakpoint.of(
			null, "100vw", _variants);

		List<ImageBreakpointVariant> imageBreakpointVariants =
			imageBreakpoint.getVariants();

		imageBreakpointVariants.add(
			ImageBreakpointVariantBuilder.url(
				"/other.jpg"
			).build());
	}

	private BreakpointPreset _autoSizesPreset() {
		return new BreakpointPreset(
			true, "default", null, null, "100vw",
			Collections.<String, String>emptyMap());
	}

	private final List<ImageBreakpointVariant> _variants = Arrays.asList(
		ImageBreakpointVariantBuilder.url(
			"/photo.jpg?width=320"
		).width(
			320
		).build(),
		ImageBreakpointVariantBuilder.url(
			"/photo.jpg?width=640"
		).width(
			640
		).build());

}