/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetGroup;
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

		List<ImagePreset> imagePresets = _presetsOf("hero");

		Assert.assertEquals(imagePresets.toString(), 2, imagePresets.size());
		Assert.assertEquals("wide", _breakpointNameOf(imagePresets, 0));
		Assert.assertEquals("default", _breakpointNameOf(imagePresets, 1));

		ImagePreset imagePreset = imagePresets.get(1);

		Assert.assertNull(imagePreset.getMediaQuery());
	}

	@Test
	public void testMaxWidthIsParsed() {
		_givenConfiguration(
			new String[0],
			new String[] {
				"thumb.default.sizes=96px", "thumb.default.maxWidth=320"
			});

		ImagePreset imagePreset = _firstPresetOf("thumb");

		Assert.assertEquals(Integer.valueOf(320), imagePreset.getMaxWidth());
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

		List<ImagePreset> imagePresets = _presetsOf("hero");

		Assert.assertEquals("narrow", _breakpointNameOf(imagePresets, 0));
		Assert.assertEquals("wide", _breakpointNameOf(imagePresets, 1));
	}

	@Test
	public void testPresetsReferencingAnUndeclaredBreakpointAreDropped() {

		// A typo would otherwise become an unconditional group that shadows
		// every group after it.

		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narow.sizes=100vw", "hero.narrow.sizes=50vw"});

		List<ImagePreset> imagePresets = _presetsOf("hero");

		Assert.assertEquals(imagePresets.toString(), 1, imagePresets.size());
		Assert.assertEquals("narrow", _breakpointNameOf(imagePresets, 0));
	}

	@Test
	public void testResolvesMediaQueryFromTheNamedBreakpoint() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {
				"hero.label=Hero", "hero.narrow.sizes=100vw",
				"hero.narrow.transformations=crop=1:1,quality=80"
			});

		ImagePresetGroup imagePresetGroup = _imagePresetResolverImpl.resolve(
			_COMPANY_ID, "hero");

		Assert.assertEquals("Hero", imagePresetGroup.getLabel());

		ImagePreset imagePreset = _firstPresetOf("hero");

		Assert.assertEquals("(max-width: 767px)", imagePreset.getMediaQuery());
		Assert.assertEquals("100vw", imagePreset.getSizes());

		Map<String, String> transformations = imagePreset.getTransformations();

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

		ImagePresetGroup imagePresetGroup = _imagePresetResolverImpl.resolve(
			_COMPANY_ID, "nonexistent");

		List<ImagePreset> imagePresets = imagePresetGroup.getPresets();

		Assert.assertEquals(imagePresets.toString(), 1, imagePresets.size());

		ImagePreset imagePreset = imagePresets.get(0);

		Assert.assertNull(imagePreset.getMediaQuery());
		Assert.assertEquals("100vw", imagePreset.getSizes());
	}

	private String _breakpointNameOf(
		List<ImagePreset> imagePresets, int index) {

		ImagePreset imagePreset = imagePresets.get(index);

		return imagePreset.getBreakpointName();
	}

	private ImagePreset _firstPresetOf(String presetGroupName) {
		List<ImagePreset> imagePresets = _presetsOf(presetGroupName);

		return imagePresets.get(0);
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

	private List<ImagePreset> _presetsOf(String presetGroupName) {
		ImagePresetGroup imagePresetGroup = _imagePresetResolverImpl.resolve(
			_COMPANY_ID, presetGroupName);

		return imagePresetGroup.getPresets();
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