/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.image.transformation.preset.BreakpointPreset;
import com.liferay.image.transformation.preset.ImagePreset;
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
			_imagePreset(
				_breakpointPreset("narrow", null, "50vw", "crop", "1:1"),
				_breakpointPreset("wide", null, "50vw", "crop", "16:9")));

		Assert.assertEquals(problems.toString(), 0, problems.size());
	}

	@Test
	public void testDifferingMaximumWidthIsNotInterchangeable() {

		// The candidates are the same picture, but a single image element
		// carries a single ladder, so the truncation cannot be expressed
		// without breakpoints.

		List<String> problems = _validate(
			_imagePreset(
				_breakpointPreset("narrow", 640, "100vw", "crop", "1:1"),
				_breakpointPreset("wide", 2560, "50vw", "crop", "1:1")));

		Assert.assertEquals(problems.toString(), 0, problems.size());
	}

	@Test
	public void testFormatSwitchIsReported() {
		List<String> problems = _validate(
			_imagePreset(
				_breakpointPreset("narrow", null, "100vw", "format", "webp"),
				_breakpointPreset("wide", null, "50vw", "format", "avif")));

		Assert.assertEquals(problems.toString(), 1, problems.size());

		String problem = problems.get(0);

		Assert.assertTrue(problem, problem.contains("output format"));
	}

	@Test
	public void testInterchangeableBreakpointsAreReported() {

		// Nothing distinguishes the sources but how wide they render, which is
		// what the sizes attribute of a single image element already says.

		List<String> problems = _validate(
			_imagePreset(
				_breakpointPreset("narrow", null, "100vw", "crop", "1:1"),
				_breakpointPreset("wide", null, "50vw", "crop", "1:1")));

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
				_imagePreset(
					_breakpointPreset("narrow", null, "100vw", "crop", "1:1"),
					_breakpointPreset("wide", null, "50vw", "crop", "16:9"))));
	}

	@Test
	public void testSinglePresetRendersImg() {

		// A crop does not make it art direction. One media condition is one
		// source, and a lone source is an image element.

		Assert.assertEquals(
			MarkupShape.IMG,
			ImageTransformationConfigurationValidator.getMarkupShape(
				_imagePreset(
					_breakpointPreset(
						"default", null, "100vw", "crop", "1:1"))));
	}

	private BreakpointPreset _breakpointPreset(
		String breakpointName, Integer maxWidth, String sizes,
		String transformationKey, String transformationValue) {

		Map<String, String> transformations = HashMapBuilder.put(
			transformationKey, transformationValue
		).build();

		String mediaQuery = null;

		if (!_NAME_DEFAULT.equals(breakpointName)) {
			mediaQuery = "(min-width: 768px)";
		}

		return new BreakpointPreset(
			false, breakpointName, maxWidth, mediaQuery, sizes,
			transformations);
	}

	private ImagePreset _imagePreset(BreakpointPreset... breakpointPresets) {
		return new ImagePreset(
			null, null, _NAME_HERO, Arrays.asList(breakpointPresets));
	}

	private List<String> _validate(ImagePreset imagePreset) {
		return ImageTransformationConfigurationValidator.validate(
			Collections.singletonMap(_NAME_HERO, imagePreset));
	}

	private static final String _NAME_DEFAULT = "default";

	private static final String _NAME_HERO = "hero";

}