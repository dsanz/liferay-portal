/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetGroup;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Sanz
 */
public class ImageTransformationConfigurationValidatorTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testArtDirectedBreakpointsAreNotReported() {
		List<String> problems = _validate(
			_imagePresetGroup(
				_imagePreset("narrow", null, "50vw", "crop", "1:1"),
				_imagePreset("wide", null, "50vw", "crop", "16:9")));

		Assert.assertEquals(problems.toString(), 0, problems.size());
	}

	@Test
	public void testDifferingMaximumWidthIsNotInterchangeable() {

		// The candidates are the same picture, but a single image element
		// carries a single ladder, so the truncation cannot be expressed
		// without breakpoints.

		List<String> problems = _validate(
			_imagePresetGroup(
				_imagePreset("narrow", 640, "100vw", "crop", "1:1"),
				_imagePreset("wide", 2560, "50vw", "crop", "1:1")));

		Assert.assertEquals(problems.toString(), 0, problems.size());
	}

	@Test
	public void testFormatSwitchIsReported() {
		List<String> problems = _validate(
			_imagePresetGroup(
				_imagePreset("narrow", null, "100vw", "format", "webp"),
				_imagePreset("wide", null, "50vw", "format", "avif")));

		Assert.assertEquals(problems.toString(), 1, problems.size());

		String problem = problems.get(0);

		Assert.assertTrue(problem, problem.contains("output format"));
	}

	@Test
	public void testInterchangeableBreakpointsAreReported() {

		// Nothing distinguishes the sources but how wide they render, which is
		// what the sizes attribute of a single image element already says.

		List<String> problems = _validate(
			_imagePresetGroup(
				_imagePreset("narrow", null, "100vw", "crop", "1:1"),
				_imagePreset("wide", null, "50vw", "crop", "1:1")));

		Assert.assertEquals(problems.toString(), 1, problems.size());

		String problem = problems.get(0);

		Assert.assertTrue(problem, problem.contains("hero"));
		Assert.assertTrue(problem, problem.contains("interchangeable"));
	}

	@Test
	public void testSeveralPresetsRenderPicture() {
		Assert.assertEquals(
			MarkupShape.PICTURE,
			ImageTransformationConfigurationValidator.getMarkupShape(
				_imagePresetGroup(
					_imagePreset("narrow", null, "100vw", "crop", "1:1"),
					_imagePreset("wide", null, "50vw", "crop", "16:9"))));
	}

	@Test
	public void testSinglePresetRendersImg() {

		// A crop does not make it art direction. One media condition is one
		// source, and a lone source is an image element.

		Assert.assertEquals(
			MarkupShape.IMG,
			ImageTransformationConfigurationValidator.getMarkupShape(
				_imagePresetGroup(
					_imagePreset("default", null, "100vw", "crop", "1:1"))));
	}

	private ImagePreset _imagePreset(
		String breakpointName, Integer maxWidth, String sizes,
		String transformationKey, String transformationValue) {

		Map<String, String> transformations = HashMapBuilder.put(
			transformationKey, transformationValue
		).build();

		String mediaQuery = null;

		if (!_NAME_DEFAULT.equals(breakpointName)) {
			mediaQuery = "(min-width: 768px)";
		}

		return new ImagePreset(
			false, breakpointName, maxWidth, mediaQuery, sizes,
			transformations);
	}

	private ImagePresetGroup _imagePresetGroup(ImagePreset... imagePresets) {
		return new ImagePresetGroup(
			null, null, _NAME_HERO, Arrays.asList(imagePresets));
	}

	private List<String> _validate(ImagePresetGroup imagePresetGroup) {
		return ImageTransformationConfigurationValidator.validate(
			Collections.singletonMap(_NAME_HERO, imagePresetGroup));
	}

	private static final String _NAME_DEFAULT = "default";

	private static final String _NAME_HERO = "hero";

}