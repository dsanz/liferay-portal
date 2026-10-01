/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.responsive.image.constants.ResponsiveImageConstants;

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
public class ResponsiveImageConfigurationRegistryTest {

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
			_responsiveImageConfigurationRegistry,
			"_responsiveImageConfigurationHelper",
			_responsiveImageConfigurationHelper);
	}

	@Test
	public void testGetPresetDefinitionArtDirectingABuiltInKeepsItsUnconditionalSource() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {"hero.narrow.sizes=50vw"});

		List<SourceDefinition> sourceDefinitions = _sourceDefinitionsOf("hero");

		Assert.assertEquals(
			sourceDefinitions.toString(), 2, sourceDefinitions.size());
		Assert.assertEquals(
			"narrow", _mediaConditionNameOf(sourceDefinitions, 0));
		Assert.assertEquals(
			"default", _mediaConditionNameOf(sourceDefinitions, 1));
	}

	@Test
	public void testGetPresetDefinitionBuiltInPresetsExistWithoutAnyConfiguration()
		throws Exception {

		_givenConfiguration(new String[0], new String[0]);

		Field[] fields = ResponsiveImageConstants.class.getFields();

		Assert.assertEquals(Arrays.toString(fields), 6, fields.length);

		for (Field field : fields) {
			String presetName = (String)field.get(null);

			PresetDefinition presetDefinition =
				_responsiveImageConfigurationRegistry.getPresetDefinition(
					0, _COMPANY_ID, presetName);

			Assert.assertEquals(
				field.getName(), presetName, presetDefinition.getName());
		}
	}

	@Test
	public void testGetPresetDefinitionBuiltInPresetsSurviveAConfigurationThatOmitsThem() {
		_givenConfiguration(
			new String[0],
			new String[] {"custom.label=Custom", "custom.default.sizes=50vw"});

		PresetDefinition presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
				0, _COMPANY_ID, "card");

		Assert.assertEquals("card", presetDefinition.getName());

		presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
				0, _COMPANY_ID, "custom");

		Assert.assertEquals("custom", presetDefinition.getName());
	}

	@Test
	public void testGetPresetDefinitionBuiltInPresetsSurviveAnUnreadableConfiguration()
		throws Exception {

		Mockito.when(
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				0, _COMPANY_ID)
		).thenReturn(
			null
		);

		for (Field field : ResponsiveImageConstants.class.getFields()) {
			String presetName = (String)field.get(null);

			PresetDefinition presetDefinition =
				_responsiveImageConfigurationRegistry.getPresetDefinition(
					0, _COMPANY_ID, presetName);

			Assert.assertEquals(
				field.getName(), presetName, presetDefinition.getName());
		}
	}

	@Test
	public void testGetPresetDefinitionConfigurationOverridesOneKeyOfABuiltInPreset() {
		_givenConfiguration(
			new String[0], new String[] {"card.default.sizes=25vw"});

		SourceDefinition sourceDefinition = _firstSourceDefinitionOf("card");

		Assert.assertEquals("25vw", sourceDefinition.getSizes(false));
		Assert.assertEquals(
			Integer.valueOf(960), sourceDefinition.getMaxWidth());

		PresetDefinition presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
				0, _COMPANY_ID, "card");

		Assert.assertEquals("Card", presetDefinition.getLabel());
	}

	@Test
	public void testGetPresetDefinitionDefaultMediaConditionSortsLastAsTheCatchAll() {
		_givenConfiguration(
			new String[] {"wide.media=(min-width: 768px)"},
			new String[] {"hero.default.sizes=100vw", "hero.wide.sizes=50vw"});

		List<SourceDefinition> sourceDefinitions = _sourceDefinitionsOf("hero");

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
	public void testGetPresetDefinitionMaxWidthIsParsed() {
		_givenConfiguration(
			new String[0],
			new String[] {
				"thumb.default.sizes=96px", "thumb.default.maxWidth=320"
			});

		SourceDefinition sourceDefinition = _firstSourceDefinitionOf("thumb");

		Assert.assertEquals(
			Integer.valueOf(320), sourceDefinition.getMaxWidth());
	}

	@Test
	public void testGetPresetDefinitionOrderingComesFromMediaConditionDeclarationNotPresetOrder() {
		_givenConfiguration(
			new String[] {
				"narrow.media=(max-width: 767px)",
				"wide.media=(min-width: 768px)"
			},
			new String[] {"hero.wide.sizes=50vw", "hero.narrow.sizes=100vw"});

		List<SourceDefinition> sourceDefinitions = _sourceDefinitionsOf("hero");

		Assert.assertEquals(
			"narrow", _mediaConditionNameOf(sourceDefinitions, 0));
		Assert.assertEquals(
			"wide", _mediaConditionNameOf(sourceDefinitions, 1));
	}

	@Test
	public void testGetPresetDefinitionPresetsReferencingAnUndeclaredMediaConditionAreDropped() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {
				"custom.narow.sizes=100vw", "custom.narrow.sizes=50vw"
			});

		List<SourceDefinition> sourceDefinitions = _sourceDefinitionsOf(
			"custom");

		Assert.assertEquals(
			sourceDefinitions.toString(), 1, sourceDefinitions.size());
		Assert.assertEquals(
			"narrow", _mediaConditionNameOf(sourceDefinitions, 0));
	}

	@Test
	public void testGetPresetDefinitionResolvesMediaConditionFromTheNamedDeclaration() {
		_givenConfiguration(
			new String[] {"narrow.media=(max-width: 767px)"},
			new String[] {
				"hero.label=Hero", "hero.narrow.sizes=100vw",
				"hero.narrow.transformations=crop=1:1,quality=80"
			});

		PresetDefinition presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
				0, _COMPANY_ID, "hero");

		Assert.assertEquals("Hero", presetDefinition.getLabel());

		SourceDefinition sourceDefinition = _firstSourceDefinitionOf("hero");

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
	public void testGetPresetDefinitionUnknownNameFallsBackToAWorkingImage() {
		_givenConfiguration(new String[0], new String[0]);

		PresetDefinition presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
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
	public void testGetPresetDefinitionUnknownNameFallsBackToTheConfiguredDefault() {
		_givenConfiguration(
			new String[0], new String[] {"default.default.sizes=42vw"});

		PresetDefinition presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
				0, _COMPANY_ID, "nonexistent");

		Assert.assertEquals(
			ResponsiveImageConstants.PRESET_DEFAULT,
			presetDefinition.getName());

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		SourceDefinition sourceDefinition = sourceDefinitions.get(0);

		Assert.assertEquals("42vw", sourceDefinition.getSizes());
	}

	@Test
	public void testGetScopedConfigurationChangingAnySettingInvalidatesTheMemo() {
		_givenConfiguration(new String[0], new String[0]);

		Mockito.when(
			_responsiveImageConfiguration.responsiveImageURLTransformerName()
		).thenReturn(
			"fastly"
		);

		ResponsiveImageConfigurationRegistry.ScopedConfiguration
			scopedConfiguration =
				_responsiveImageConfigurationRegistry.getScopedConfiguration(
					0, _COMPANY_ID);

		Assert.assertEquals(
			"fastly",
			scopedConfiguration.getResponsiveImageURLTransformerName());

		Mockito.when(
			_responsiveImageConfiguration.responsiveImageURLTransformerName()
		).thenReturn(
			""
		);

		scopedConfiguration =
			_responsiveImageConfigurationRegistry.getScopedConfiguration(
				0, _COMPANY_ID);

		Assert.assertEquals(
			"", scopedConfiguration.getResponsiveImageURLTransformerName());
	}

	private SourceDefinition _firstSourceDefinitionOf(String presetName) {
		List<SourceDefinition> sourceDefinitions = _sourceDefinitionsOf(
			presetName);

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

	private List<SourceDefinition> _sourceDefinitionsOf(String presetName) {
		PresetDefinition presetDefinition =
			_responsiveImageConfigurationRegistry.getPresetDefinition(
				0, _COMPANY_ID, presetName);

		return presetDefinition.getSourceDefinitions();
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private final ResponsiveImageConfiguration _responsiveImageConfiguration =
		Mockito.mock(ResponsiveImageConfiguration.class);
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);
	private final ResponsiveImageConfigurationRegistry
		_responsiveImageConfigurationRegistry =
			new ResponsiveImageConfigurationRegistry(null, null);

}