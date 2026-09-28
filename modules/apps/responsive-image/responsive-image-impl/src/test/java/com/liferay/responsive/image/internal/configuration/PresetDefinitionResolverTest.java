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
	public void testDefaultMediaConditionSortsLastAsTheCatchAll() {
		_givenConfiguration(
			new String[] {"wide.media=(min-width: 768px)"},
			new String[] {"hero.default.sizes=100vw", "hero.wide.sizes=50vw"});

		List<SourceDefinition> sourceDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			sourceDefinitions.toString(), 2, sourceDefinitions.size());
		Assert.assertEquals(
			"wide", _mediaConditionNameOf(sourceDefinitions, 0));
		Assert.assertEquals(
			"default", _mediaConditionNameOf(sourceDefinitions, 1));

		SourceDefinition sourceDefinition = sourceDefinitions.get(1);

		Assert.assertNull(sourceDefinition.getMediaQuery());
	}

	@Test
	public void testMaxWidthIsParsed() {
		_givenConfiguration(
			new String[0],
			new String[] {
				"thumb.default.sizes=96px", "thumb.default.maxWidth=320"
			});

		SourceDefinition sourceDefinition = _firstPresetOf("thumb");

		Assert.assertEquals(
			Integer.valueOf(320), sourceDefinition.getMaxWidth());
	}

	@Test
	public void testOrderingComesFromMediaConditionDeclarationNotPresetOrder() {

		// Source matching is first wins, so this ordering decides which image
		// a browser picks. Declaring it once for the installation is what stops
		// a reordered preset list changing rendering silently.

		_givenConfiguration(
			new String[] {
				"narrow.media=(max-width: 767px)",
				"wide.media=(min-width: 768px)"
			},
			new String[] {"hero.wide.sizes=50vw", "hero.narrow.sizes=100vw"});

		List<SourceDefinition> sourceDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			"narrow", _mediaConditionNameOf(sourceDefinitions, 0));
		Assert.assertEquals(
			"wide", _mediaConditionNameOf(sourceDefinitions, 1));
	}

	@Test
	public void testPresetsReferencingAnUndeclaredMediaConditionAreDropped() {

		// A typo would otherwise become an unconditional source that
		// shadows every source after it.

		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narow.sizes=100vw", "hero.narrow.sizes=50vw"});

		List<SourceDefinition> sourceDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			sourceDefinitions.toString(), 1, sourceDefinitions.size());
		Assert.assertEquals(
			"narrow", _mediaConditionNameOf(sourceDefinitions, 0));
	}

	@Test
	public void testResolvesMediaConditionFromTheNamedDeclaration() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {
				"hero.label=Hero", "hero.narrow.sizes=100vw",
				"hero.narrow.transformations=crop=1:1,quality=80"
			});

		PresetDefinition presetDefinition = _presetDefinitionResolver.resolve(
			0, _COMPANY_ID, "hero");

		Assert.assertEquals("Hero", presetDefinition.getLabel());

		SourceDefinition sourceDefinition = _firstPresetOf("hero");

		Assert.assertEquals(
			"(max-width: 767px)", sourceDefinition.getMediaQuery());
		Assert.assertEquals("100vw", sourceDefinition.getSizes());

		Map<String, String> transformations =
			sourceDefinition.getTransformations();

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

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		Assert.assertEquals(
			sourceDefinitions.toString(), 1, sourceDefinitions.size());

		SourceDefinition sourceDefinition = sourceDefinitions.get(0);

		Assert.assertNull(sourceDefinition.getMediaQuery());
		Assert.assertEquals("100vw", sourceDefinition.getSizes());
	}

	private SourceDefinition _firstPresetOf(String presetName) {
		List<SourceDefinition> sourceDefinitions = _presetsOf(presetName);

		return sourceDefinitions.get(0);
	}

	private void _givenConfiguration(
		String[] mediaConditions, String[] presets) {

		Mockito.when(
			_responsiveImageConfiguration.mediaConditions()
		).thenReturn(
			mediaConditions
		);

		Mockito.when(
			_responsiveImageConfiguration.presets()
		).thenReturn(
			presets
		);
	}

	private String _mediaConditionNameOf(
		List<SourceDefinition> sourceDefinitions, int index) {

		SourceDefinition sourceDefinition = sourceDefinitions.get(index);

		return sourceDefinition.getMediaConditionName();
	}

	private List<SourceDefinition> _presetsOf(String presetName) {
		PresetDefinition presetDefinition = _presetDefinitionResolver.resolve(
			0, _COMPANY_ID, presetName);

		return presetDefinition.getSourceDefinitions();
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