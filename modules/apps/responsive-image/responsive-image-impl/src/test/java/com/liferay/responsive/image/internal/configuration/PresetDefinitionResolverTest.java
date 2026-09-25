/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

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
public class PresetDefinitionResolverTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		Mockito.when(
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				0, _COMPANY_ID)
		).thenReturn(
			_responsiveImageConfiguration
		);

		ReflectionTestUtil.setFieldValue(
			_presetDefinitionResolver, "_responsiveImageConfigurationHelper",
			_responsiveImageConfigurationHelper);
	}

	@Test
	public void testDefaultBreakpointSortsLastAsTheCatchAll() {
		_givenConfiguration(
			new String[] {"wide.media=(min-width: 768px)"},
			new String[] {"hero.default.sizes=100vw", "hero.wide.sizes=50vw"});

		List<BreakpointDefinition> breakpointDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			breakpointDefinitions.toString(), 2, breakpointDefinitions.size());
		Assert.assertEquals(
			"wide", _breakpointNameOf(breakpointDefinitions, 0));
		Assert.assertEquals(
			"default", _breakpointNameOf(breakpointDefinitions, 1));

		BreakpointDefinition breakpointDefinition = breakpointDefinitions.get(
			1);

		Assert.assertNull(breakpointDefinition.getMediaQuery());
	}

	@Test
	public void testMaxWidthIsParsed() {
		_givenConfiguration(
			new String[0],
			new String[] {
				"thumb.default.sizes=96px", "thumb.default.maxWidth=320"
			});

		BreakpointDefinition breakpointDefinition = _firstPresetOf("thumb");

		Assert.assertEquals(
			Integer.valueOf(320), breakpointDefinition.getMaxWidth());
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

		List<BreakpointDefinition> breakpointDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			"narrow", _breakpointNameOf(breakpointDefinitions, 0));
		Assert.assertEquals(
			"wide", _breakpointNameOf(breakpointDefinitions, 1));
	}

	@Test
	public void testPresetsReferencingAnUndeclaredBreakpointAreDropped() {

		// A typo would otherwise become an unconditional breakpoint that
		// shadows every breakpoint after it.

		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narow.sizes=100vw", "hero.narrow.sizes=50vw"});

		List<BreakpointDefinition> breakpointDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			breakpointDefinitions.toString(), 1, breakpointDefinitions.size());
		Assert.assertEquals(
			"narrow", _breakpointNameOf(breakpointDefinitions, 0));
	}

	@Test
	public void testResolvesMediaQueryFromTheNamedBreakpoint() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {
				"hero.label=Hero", "hero.narrow.sizes=100vw",
				"hero.narrow.transformations=crop=1:1,quality=80"
			});

		PresetDefinition presetDefinition = _presetDefinitionResolver.resolve(
			0, _COMPANY_ID, "hero");

		Assert.assertEquals("Hero", presetDefinition.getLabel());

		BreakpointDefinition breakpointDefinition = _firstPresetOf("hero");

		Assert.assertEquals(
			"(max-width: 767px)", breakpointDefinition.getMediaQuery());
		Assert.assertEquals("100vw", breakpointDefinition.getSizes());

		Map<String, String> transformations =
			breakpointDefinition.getTransformations();

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

		PresetDefinition presetDefinition = _presetDefinitionResolver.resolve(
			0, _COMPANY_ID, "nonexistent");

		List<BreakpointDefinition> breakpointDefinitions =
			presetDefinition.getBreakpointDefinitions();

		Assert.assertEquals(
			breakpointDefinitions.toString(), 1, breakpointDefinitions.size());

		BreakpointDefinition breakpointDefinition = breakpointDefinitions.get(
			0);

		Assert.assertNull(breakpointDefinition.getMediaQuery());
		Assert.assertEquals("100vw", breakpointDefinition.getSizes());
	}

	private String _breakpointNameOf(
		List<BreakpointDefinition> breakpointDefinitions, int index) {

		BreakpointDefinition breakpointDefinition = breakpointDefinitions.get(
			index);

		return breakpointDefinition.getBreakpointName();
	}

	private BreakpointDefinition _firstPresetOf(String presetName) {
		List<BreakpointDefinition> breakpointDefinitions = _presetsOf(
			presetName);

		return breakpointDefinitions.get(0);
	}

	private void _givenConfiguration(String[] breakpoints, String[] presets) {
		Mockito.when(
			_responsiveImageConfiguration.breakpoints()
		).thenReturn(
			breakpoints
		);

		Mockito.when(
			_responsiveImageConfiguration.presets()
		).thenReturn(
			presets
		);
	}

	private List<BreakpointDefinition> _presetsOf(String presetName) {
		PresetDefinition presetDefinition = _presetDefinitionResolver.resolve(
			0, _COMPANY_ID, presetName);

		return presetDefinition.getBreakpointDefinitions();
	}

	private static final long _COMPANY_ID = 42L;

	private final PresetDefinitionResolver _presetDefinitionResolver =
		new PresetDefinitionResolver(null, null);
	private final ResponsiveImageConfiguration _responsiveImageConfiguration =
		Mockito.mock(ResponsiveImageConfiguration.class);
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);

}