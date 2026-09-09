/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetBreakpoint;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Daniel Sanz
 */
public class ImagePresetResolverImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		Mockito.when(
			_imageTransformationConfigurationHelper.
				getImageTransformationConfiguration(_COMPANY_ID)
		).thenReturn(
			_imageTransformationConfiguration
		);

		ReflectionTestUtil.setFieldValue(
			_imagePresetResolverImpl, "_imageTransformationConfigurationHelper",
			_imageTransformationConfigurationHelper);
	}

	@Test
	public void testDefaultBreakpointSortsLastAsTheCatchAll() {
		_givenConfiguration(
			new String[] {"wide.media=(min-width: 768px)"},
			new String[] {"hero.default.sizes=100vw", "hero.wide.sizes=50vw"});

		List<ImagePresetBreakpoint> imagePresetBreakpoints = _presetsOf("hero");

		Assert.assertEquals(
			imagePresetBreakpoints.toString(), 2,
			imagePresetBreakpoints.size());
		Assert.assertEquals(
			"wide", _breakpointNameOf(imagePresetBreakpoints, 0));
		Assert.assertEquals(
			"default", _breakpointNameOf(imagePresetBreakpoints, 1));

		ImagePresetBreakpoint imagePresetBreakpoint =
			imagePresetBreakpoints.get(1);

		Assert.assertNull(imagePresetBreakpoint.getMediaQuery());
	}

	@Test
	public void testMaxWidthIsParsed() {
		_givenConfiguration(
			new String[0],
			new String[] {
				"thumb.default.sizes=96px", "thumb.default.maxWidth=320"
			});

		ImagePresetBreakpoint imagePresetBreakpoint = _firstPresetOf("thumb");

		Assert.assertEquals(
			Integer.valueOf(320), imagePresetBreakpoint.getMaxWidth());
	}

	@Test
	public void testOrderingComesFromBreakpointDeclarationNotPresetOrder() {

		// Source matching is first wins, so this ordering decides which image
		// a browser picks. Declaring it once for the installation is what stops
		// a reordered preset list changing rendering silently.

		_givenConfiguration(
			new String[] {
				"narrow.media=(max-width: 767px)",
				"wide.media=(min-width: 768px)"
			},
			new String[] {"hero.wide.sizes=50vw", "hero.narrow.sizes=100vw"});

		List<ImagePresetBreakpoint> imagePresetBreakpoints = _presetsOf("hero");

		Assert.assertEquals(
			"narrow", _breakpointNameOf(imagePresetBreakpoints, 0));
		Assert.assertEquals(
			"wide", _breakpointNameOf(imagePresetBreakpoints, 1));
	}

	@Test
	public void testPresetsReferencingAnUndeclaredBreakpointAreDropped() {

		// A typo would otherwise become an unconditional breakpoint that
		// shadows every breakpoint after it.

		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narow.sizes=100vw", "hero.narrow.sizes=50vw"});

		List<ImagePresetBreakpoint> imagePresetBreakpoints = _presetsOf("hero");

		Assert.assertEquals(
			imagePresetBreakpoints.toString(), 1,
			imagePresetBreakpoints.size());
		Assert.assertEquals(
			"narrow", _breakpointNameOf(imagePresetBreakpoints, 0));
	}

	@Test
	public void testResolvesMediaQueryFromTheNamedBreakpoint() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {
				"hero.label=Hero", "hero.narrow.sizes=100vw",
				"hero.narrow.transformations=crop=1:1,quality=80"
			});

		ImagePreset imagePreset = _imagePresetResolverImpl.resolve(
			_COMPANY_ID, "hero");

		Assert.assertEquals("Hero", imagePreset.getLabel());

		ImagePresetBreakpoint imagePresetBreakpoint = _firstPresetOf("hero");

		Assert.assertEquals(
			"(max-width: 767px)", imagePresetBreakpoint.getMediaQuery());
		Assert.assertEquals("100vw", imagePresetBreakpoint.getSizes());

		Map<String, String> transformations =
			imagePresetBreakpoint.getTransformations();

		Assert.assertEquals(
			transformations.toString(), 2, transformations.size());
		Assert.assertEquals("1:1", transformations.get("crop"));
		Assert.assertEquals("80", transformations.get("quality"));
	}

	@Test
	public void testUnknownGroupFallsBackToAWorkingImage() {

		// A typo in configuration should degrade to a plain full width image
		// rather than to no image.

		_givenConfiguration(new String[0], new String[0]);

		ImagePreset imagePreset = _imagePresetResolverImpl.resolve(
			_COMPANY_ID, "nonexistent");

		List<ImagePresetBreakpoint> imagePresetBreakpoints =
			imagePreset.getBreakpoints();

		Assert.assertEquals(
			imagePresetBreakpoints.toString(), 1,
			imagePresetBreakpoints.size());

		ImagePresetBreakpoint imagePresetBreakpoint =
			imagePresetBreakpoints.get(0);

		Assert.assertNull(imagePresetBreakpoint.getMediaQuery());
		Assert.assertEquals("100vw", imagePresetBreakpoint.getSizes());
	}

	private String _breakpointNameOf(
		List<ImagePresetBreakpoint> imagePresetBreakpoints, int index) {

		ImagePresetBreakpoint imagePresetBreakpoint =
			imagePresetBreakpoints.get(index);

		return imagePresetBreakpoint.getBreakpointName();
	}

	private ImagePresetBreakpoint _firstPresetOf(String presetName) {
		List<ImagePresetBreakpoint> imagePresetBreakpoints = _presetsOf(
			presetName);

		return imagePresetBreakpoints.get(0);
	}

	private void _givenConfiguration(String[] breakpoints, String[] presets) {
		Mockito.when(
			_imageTransformationConfiguration.breakpoints()
		).thenReturn(
			breakpoints
		);

		Mockito.when(
			_imageTransformationConfiguration.presets()
		).thenReturn(
			presets
		);
	}

	private List<ImagePresetBreakpoint> _presetsOf(String presetName) {
		ImagePreset imagePreset = _imagePresetResolverImpl.resolve(
			_COMPANY_ID, presetName);

		return imagePreset.getBreakpoints();
	}

	private static final long _COMPANY_ID = 42L;

	private final ImagePresetResolverImpl _imagePresetResolverImpl =
		new ImagePresetResolverImpl();
	private final ImageTransformationConfiguration
		_imageTransformationConfiguration = Mockito.mock(
			ImageTransformationConfiguration.class);
	private final ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper = Mockito.mock(
			ImageTransformationConfigurationHelper.class);

}