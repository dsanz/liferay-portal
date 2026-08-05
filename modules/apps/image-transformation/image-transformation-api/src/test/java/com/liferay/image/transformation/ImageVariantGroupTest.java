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
public class ImageVariantGroupTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testFromCarriesPresetLayoutThrough() {

		// The single crossing point from configuration into output. A provider
		// that drifted from the preset it was asked to honor would show up
		// here first.

		ImagePreset imagePreset = new ImagePreset(
			"narrow", 320, "(max-width: 767px)", "100vw",
			HashMapBuilder.put(
				"crop", "1:1"
			).build());

		ImageVariantGroup imageVariantGroup = ImageVariantGroup.from(
			imagePreset, _variants);

		Assert.assertEquals(
			"(max-width: 767px)", imageVariantGroup.getMediaQuery());
		Assert.assertEquals("100vw", imageVariantGroup.getSizes());
		Assert.assertEquals(_variants, imageVariantGroup.getVariants());
	}

	@Test
	public void testOfAllowsAProviderToDetermineItsOwnGroup() {

		// Adaptive Media never sees a preset: its media conditions come from
		// its own configuration entries, and a single candidate group has no
		// sizes to disambiguate.

		ImageVariantGroup imageVariantGroup = ImageVariantGroup.of(
			"(max-width: 640px)", null, _variants);

		Assert.assertEquals(
			"(max-width: 640px)", imageVariantGroup.getMediaQuery());
		Assert.assertNull(imageVariantGroup.getSizes());
	}

	@Test
	public void testUnconditionalGroupHasNoMediaQuery() {

		// One unconditional group is what renders as a plain img rather than
		// as a picture element.

		ImageVariantGroup imageVariantGroup = ImageVariantGroup.of(
			null, "100vw", Collections.<ImageVariant>emptyList());

		Assert.assertNull(imageVariantGroup.getMediaQuery());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testVariantsAreUnmodifiable() {
		ImageVariantGroup imageVariantGroup = ImageVariantGroup.of(
			null, "100vw", _variants);

		List<ImageVariant> imageVariants = imageVariantGroup.getVariants();

		imageVariants.add(
			ImageVariant.builder(
				"/other.jpg"
			).build());
	}

	private final List<ImageVariant> _variants = Arrays.asList(
		ImageVariant.builder(
			"/photo.jpg?width=320"
		).width(
			320
		).build(),
		ImageVariant.builder(
			"/photo.jpg?width=640"
		).width(
			640
		).build());

}