/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.responsive.image.constants.ResponsiveImagePresetConstants;

import java.lang.reflect.Field;

import java.util.Arrays;
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
public class PresetDefinitionRegistryTest {

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
			_presetDefinitionRegistry, "_responsiveImageConfigurationHelper",
			_responsiveImageConfigurationHelper);
	}

	@Test
	public void testArtDirectingABuiltInKeepsItsUnconditionalSource() {

		// The built in entry becomes the catch all, which is what a browser
		// falls back to when no media condition matches.

		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narrow.sizes=50vw"});

		List<SourceDefinition> sourceDefinitions = _presetsOf("hero");

		Assert.assertEquals(
			sourceDefinitions.toString(), 2, sourceDefinitions.size());
		Assert.assertEquals(
			"narrow", _mediaConditionNameOf(sourceDefinitions, 0));
		Assert.assertEquals(
			"default", _mediaConditionNameOf(sourceDefinitions, 1));
	}

	@Test
	public void testBuiltInPresetsExistWithoutAnyConfiguration()
		throws Exception {

		// Every published constant has to resolve, or a rendering surface
		// naming one silently falls back to a different size. Read from the
		// class rather than a list here, so publishing a constant without
		// seeding the preset fails immediately.

		_givenConfiguration(new String[0], new String[0]);

		Field[] fields = ResponsiveImagePresetConstants.class.getFields();

		Assert.assertEquals(Arrays.toString(fields), 6, fields.length);

		for (Field field : fields) {
			String presetName = (String)field.get(null);

			PresetDefinition presetDefinition =
				_presetDefinitionRegistry.getPresetDefinition(
					0, _COMPANY_ID, presetName);

			Assert.assertEquals(
				field.getName(), presetName, presetDefinition.getName());
		}
	}

	@Test
	public void testBuiltInPresetsSurviveAConfigurationThatOmitsThem() {

		// There is no way to spell a removal, which is what makes the
		// guarantee hold. Configuring one preset must not drop the rest.

		_givenConfiguration(
			new String[0],
			new String[] {"custom.label=Custom", "custom.default.sizes=50vw"});

		PresetDefinition presetDefinition =
			_presetDefinitionRegistry.getPresetDefinition(
				0, _COMPANY_ID, "card");

		Assert.assertEquals("card", presetDefinition.getName());

		presetDefinition = _presetDefinitionRegistry.getPresetDefinition(
			0, _COMPANY_ID, "custom");

		Assert.assertEquals("custom", presetDefinition.getName());
	}

	@Test
	public void testBuiltInPresetsSurviveAnUnreadableConfiguration()
		throws Exception {

		// The helper answers null when the configuration is absent or could
		// not be read. Losing the built ins there would break the guarantee
		// exactly when a caller most needs it to hold.

		Mockito.when(
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				0, _COMPANY_ID)
		).thenReturn(
			null
		);

		for (Field field : ResponsiveImagePresetConstants.class.getFields()) {
			String presetName = (String)field.get(null);

			PresetDefinition presetDefinition =
				_presetDefinitionRegistry.getPresetDefinition(
					0, _COMPANY_ID, presetName);

			Assert.assertEquals(
				field.getName(), presetName, presetDefinition.getName());
		}
	}

	@Test
	public void testConfigurationOverridesOneKeyOfABuiltInPreset() {

		// Everything not restated has to keep coming from the built in, or a
		// site changing one value stops inheriting later changes to the rest.

		_givenConfiguration(
			new String[0], new String[] {"card.default.sizes=25vw"});

		SourceDefinition sourceDefinition = _firstPresetOf("card");

		Assert.assertEquals("25vw", sourceDefinition.getSizes(false));
		Assert.assertEquals(
			Integer.valueOf(960), sourceDefinition.getMaxWidth());

		PresetDefinition presetDefinition =
			_presetDefinitionRegistry.getPresetDefinition(
				0, _COMPANY_ID, "card");

		Assert.assertEquals("Card", presetDefinition.getLabel());
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
			new String[] {
				"custom.narow.sizes=100vw", "custom.narrow.sizes=50vw"
			});

		List<SourceDefinition> sourceDefinitions = _presetsOf("custom");

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

		PresetDefinition presetDefinition =
			_presetDefinitionRegistry.getPresetDefinition(
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
	public void testUnknownNameFallsBackToAWorkingImage() {

		// A typo in configuration should degrade to a plain full width image
		// rather than to no image.

		_givenConfiguration(new String[0], new String[0]);

		PresetDefinition presetDefinition =
			_presetDefinitionRegistry.getPresetDefinition(
				0, _COMPANY_ID, "nonexistent");

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		Assert.assertEquals(
			sourceDefinitions.toString(), 1, sourceDefinitions.size());

		SourceDefinition sourceDefinition = sourceDefinitions.get(0);

		Assert.assertNull(sourceDefinition.getMediaQuery());
		Assert.assertEquals("100vw", sourceDefinition.getSizes());
	}

	@Test
	public void testUnknownNameFallsBackToTheConfiguredDefault() {

		// Falling back to the scope's own default rather than to a hardcoded
		// one, so a site that said what its unclassified images look like gets
		// that answer for a name nobody declared too.

		_givenConfiguration(
			new String[0], new String[] {"default.default.sizes=42vw"});

		PresetDefinition presetDefinition =
			_presetDefinitionRegistry.getPresetDefinition(
				0, _COMPANY_ID, "nonexistent");

		Assert.assertEquals(
			ResponsiveImagePresetConstants.DEFAULT, presetDefinition.getName());

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		SourceDefinition sourceDefinition = sourceDefinitions.get(0);

		Assert.assertEquals("42vw", sourceDefinition.getSizes());
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
		PresetDefinition presetDefinition =
			_presetDefinitionRegistry.getPresetDefinition(
				0, _COMPANY_ID, presetName);

		return presetDefinition.getSourceDefinitions();
	}

	private static final long _COMPANY_ID = 42L;

	private final PresetDefinitionRegistry _presetDefinitionRegistry =
		new PresetDefinitionRegistry(null, null);
	private final ResponsiveImageConfiguration _responsiveImageConfiguration =
		Mockito.mock(ResponsiveImageConfiguration.class);
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);

}