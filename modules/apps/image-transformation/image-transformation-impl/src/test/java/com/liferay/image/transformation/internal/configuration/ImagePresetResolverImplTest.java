/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.image.transformation.preset.BreakpointPreset;
import com.liferay.image.transformation.preset.ImagePreset;
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
				getImageTransformationConfiguration(0, _COMPANY_ID)
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

		List<BreakpointPreset> breakpointPresets = _presetsOf("hero");

		Assert.assertEquals(
			breakpointPresets.toString(), 2, breakpointPresets.size());
		Assert.assertEquals("wide", _breakpointNameOf(breakpointPresets, 0));
		Assert.assertEquals("default", _breakpointNameOf(breakpointPresets, 1));

		BreakpointPreset breakpointPreset = breakpointPresets.get(1);

		Assert.assertNull(breakpointPreset.getMediaQuery());
	}

	@Test
	public void testMaxWidthIsParsed() {
		_givenConfiguration(
			new String[0],
			new String[] {
				"thumb.default.sizes=96px", "thumb.default.maxWidth=320"
			});

		BreakpointPreset breakpointPreset = _firstPresetOf("thumb");

		Assert.assertEquals(
			Integer.valueOf(320), breakpointPreset.getMaxWidth());
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

		List<BreakpointPreset> breakpointPresets = _presetsOf("hero");

		Assert.assertEquals("narrow", _breakpointNameOf(breakpointPresets, 0));
		Assert.assertEquals("wide", _breakpointNameOf(breakpointPresets, 1));
	}

	@Test
	public void testPresetsReferencingAnUndeclaredBreakpointAreDropped() {

		// A typo would otherwise become an unconditional breakpoint that
		// shadows every breakpoint after it.

		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narow.sizes=100vw", "hero.narrow.sizes=50vw"});

		List<BreakpointPreset> breakpointPresets = _presetsOf("hero");

		Assert.assertEquals(
			breakpointPresets.toString(), 1, breakpointPresets.size());
		Assert.assertEquals("narrow", _breakpointNameOf(breakpointPresets, 0));
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
			0, _COMPANY_ID, "hero");

		Assert.assertEquals("Hero", imagePreset.getLabel());

		BreakpointPreset breakpointPreset = _firstPresetOf("hero");

		Assert.assertEquals(
			"(max-width: 767px)", breakpointPreset.getMediaQuery());
		Assert.assertEquals("100vw", breakpointPreset.getSizes());

		Map<String, String> transformations =
			breakpointPreset.getTransformations();

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
			0, _COMPANY_ID, "nonexistent");

		List<BreakpointPreset> breakpointPresets =
			imagePreset.getBreakpointPresets();

		Assert.assertEquals(
			breakpointPresets.toString(), 1, breakpointPresets.size());

		BreakpointPreset breakpointPreset = breakpointPresets.get(0);

		Assert.assertNull(breakpointPreset.getMediaQuery());
		Assert.assertEquals("100vw", breakpointPreset.getSizes());
	}

	private String _breakpointNameOf(
		List<BreakpointPreset> breakpointPresets, int index) {

		BreakpointPreset breakpointPreset = breakpointPresets.get(index);

		return breakpointPreset.getBreakpointName();
	}

	private BreakpointPreset _firstPresetOf(String presetName) {
		List<BreakpointPreset> breakpointPresets = _presetsOf(presetName);

		return breakpointPresets.get(0);
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

	private List<BreakpointPreset> _presetsOf(String presetName) {
		ImagePreset imagePreset = _imagePresetResolverImpl.resolve(
			0, _COMPANY_ID, presetName);

		return imagePreset.getBreakpointPresets();
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