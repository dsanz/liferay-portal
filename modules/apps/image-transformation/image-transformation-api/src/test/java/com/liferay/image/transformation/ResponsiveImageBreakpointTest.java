/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

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
public class ResponsiveImageBreakpointTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testAutoSizesIsIgnoredWhenNotLazy() {

		// A browser only honors sizes="auto" on a lazily loaded image, so
		// emitting it here would be an invalid attribute rather than a hint.

		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			ResponsiveImageBreakpoint.from(
				_autoSizesPreset(), _variants, false);

		Assert.assertEquals("100vw", responsiveImageBreakpoint.getSizes());
	}

	@Test
	public void testAutoSizesKeepsDeclaredSizesAsFallback() {

		// The declared value stays behind the keyword so that a browser
		// without automatic sizing still receives a real one.

		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			ResponsiveImageBreakpoint.from(_autoSizesPreset(), _variants, true);

		Assert.assertEquals(
			"auto, 100vw", responsiveImageBreakpoint.getSizes());
	}

	@Test
	public void testFromCarriesPresetLayoutThrough() {

		// The single crossing point from configuration into output. A provider
		// that drifted from the preset it was asked to honor would show up
		// here first.

		ImagePresetBreakpoint imagePresetBreakpoint = new ImagePresetBreakpoint(
			false, "narrow", 320, "(max-width: 767px)", "100vw",
			HashMapBuilder.put(
				"crop", "1:1"
			).build());

		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			ResponsiveImageBreakpoint.from(
				imagePresetBreakpoint, _variants, false);

		Assert.assertEquals(
			"(max-width: 767px)", responsiveImageBreakpoint.getMediaQuery());
		Assert.assertEquals("100vw", responsiveImageBreakpoint.getSizes());
		Assert.assertEquals(_variants, responsiveImageBreakpoint.getVariants());
	}

	@Test
	public void testOfAllowsAProviderToDetermineItsOwnGroup() {

		// Adaptive Media never sees a preset: its media conditions come from
		// its own configuration entries, and a breakpoint of one candidate
		// has no sizes to disambiguate.

		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			ResponsiveImageBreakpoint.of("(max-width: 640px)", null, _variants);

		Assert.assertEquals(
			"(max-width: 640px)", responsiveImageBreakpoint.getMediaQuery());
		Assert.assertNull(responsiveImageBreakpoint.getSizes());
	}

	@Test
	public void testUnconditionalGroupHasNoMediaQuery() {

		// One unconditional breakpoint is what renders as a plain img rather
		// than as a picture element.

		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			ResponsiveImageBreakpoint.of(
				null, "100vw",
				Collections.<ResponsiveImageBreakpointVariant>emptyList());

		Assert.assertNull(responsiveImageBreakpoint.getMediaQuery());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testVariantsAreUnmodifiable() {
		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			ResponsiveImageBreakpoint.of(null, "100vw", _variants);

		List<ResponsiveImageBreakpointVariant>
			responsiveImageBreakpointVariants =
				responsiveImageBreakpoint.getVariants();

		responsiveImageBreakpointVariants.add(
			ResponsiveImageBreakpointVariantBuilder.url(
				"/other.jpg"
			).build());
	}

	private ImagePresetBreakpoint _autoSizesPreset() {
		return new ImagePresetBreakpoint(
			true, "default", null, null, "100vw",
			Collections.<String, String>emptyMap());
	}

	private final List<ResponsiveImageBreakpointVariant> _variants =
		Arrays.asList(
			ResponsiveImageBreakpointVariantBuilder.url(
				"/photo.jpg?width=320"
			).width(
				320
			).build(),
			ResponsiveImageBreakpointVariantBuilder.url(
				"/photo.jpg?width=640"
			).width(
				640
			).build());

}