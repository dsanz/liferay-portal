/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.FeatureFlag;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageBreakpoint;
import com.liferay.responsive.image.ResponsiveImageBreakpointVariant;
import com.liferay.responsive.image.ResponsiveImageBreakpointVariantBuilder;
import com.liferay.responsive.image.ResponsiveImageProvider;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageRequestBuilder;
import com.liferay.responsive.image.internal.configuration.BreakpointDefinition;
import com.liferay.responsive.image.internal.configuration.PresetDefinition;
import com.liferay.responsive.image.internal.configuration.PresetDefinitionResolver;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * Wires the renderer to a mocked provider, so that what is asserted is the
 * markup built from a model rather than anything a particular provider does.
 *
 * @author Daniel Sanz
 */
@FeatureFlag("LPD-94784")
public class ResponsiveImageMarkupRendererImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		Mockito.when(
			_imageResource.getURL()
		).thenReturn(
			"/documents/1/2/photo.jpg"
		);

		Mockito.when(
			_responsiveImageProviderSelector.getResponsiveImageProvider(
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			_responsiveImageProvider
		);

		// Null means "I do not render my own markup", which is what every
		// provider but Adaptive Media says.

		Mockito.when(
			_responsiveImageProvider.render(
				Mockito.anyString(), Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			null
		);

		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl, "_presetDefinitionResolver",
			_presetDefinitionResolver);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl,
			"_responsiveImageConfigurationHelper",
			_responsiveImageConfigurationHelper);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl,
			"_responsiveImageProviderSelector",
			_responsiveImageProviderSelector);
	}

	@Test
	public void testPassesThroughWhenNothingWasTransformed() throws Exception {

		// Every width built the same URL, so no renderer is bound. Emitting the
		// descriptors would have the browser trust them and pick wrong.

		_givenPreset(_breakpointDefinition(null, "100vw"));
		_givenResponsiveImage(
			ResponsiveImageBreakpoint.of(
				null, "100vw",
				Arrays.asList(
					_variant("/documents/1/2/photo.jpg", 320),
					_variant("/documents/1/2/photo.jpg", 640))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertEquals(_ORIGINAL_IMG_TAG, markup);
	}

	@FeatureFlag(enable = false, value = "LPD-94784")
	@Test
	public void testPassesThroughWhenTheFeatureFlagIsDisabled()
		throws Exception {

		_givenPreset(_breakpointDefinition(null, "100vw"));
		_givenResponsiveImage(
			ResponsiveImageBreakpoint.of(
				null, "100vw",
				Arrays.asList(
					_variant("/documents/1/2/photo.jpg?width=320", 320),
					_variant("/documents/1/2/photo.jpg?width=640", 640))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertEquals(_ORIGINAL_IMG_TAG, markup);

		Mockito.verifyNoInteractions(_responsiveImageProviderSelector);
	}

	@Test
	public void testPassesThroughWhenTheProviderRendersItsOwnMarkup()
		throws Exception {

		Mockito.when(
			_responsiveImageProvider.render(
				Mockito.anyString(), Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			"<picture>adaptive media</picture>"
		);

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertEquals("<picture>adaptive media</picture>", markup);
	}

	@Test
	public void testRendersImgForASingleBreakpoint() throws Exception {
		_givenPreset(_breakpointDefinition(null, "100vw"));
		_givenResponsiveImage(
			ResponsiveImageBreakpoint.of(
				null, "100vw",
				Arrays.asList(
					_variant("/documents/1/2/photo.jpg?width=320", 320),
					_variant("/documents/1/2/photo.jpg?width=640", 640))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertTrue(markup, markup.startsWith("<img "));
		Assert.assertFalse(markup, markup.contains("<picture>"));
		Assert.assertTrue(markup, markup.contains("sizes=\"100vw\""));
		Assert.assertFalse(markup, markup.contains("loading="));
		Assert.assertTrue(markup, markup.contains("alt=\"A photo\""));
		Assert.assertTrue(
			markup,
			markup.contains("/documents/1/2/photo.jpg?width=320 320w, "));
	}

	@Test
	public void testRendersPictureForArtDirection() throws Exception {

		// Two breakpoints mean the crop differs by viewport, which srcset alone
		// cannot express.

		_givenPreset(
			_breakpointDefinition("(max-width: 767px)", "100vw"),
			_breakpointDefinition("(min-width: 768px)", "50vw"));
		_givenResponsiveImage(
			ResponsiveImageBreakpoint.of(
				"(max-width: 767px)", "100vw",
				Collections.singletonList(
					_variant("/documents/1/2/photo.jpg?crop=1%3A1", 320))),
			ResponsiveImageBreakpoint.of(
				"(min-width: 768px)", "50vw",
				Collections.singletonList(
					_variant("/documents/1/2/photo.jpg?crop=16%3A9", 960))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertTrue(markup, markup.startsWith("<picture>"));
		Assert.assertTrue(markup, markup.endsWith("</picture>"));
		Assert.assertTrue(
			markup, markup.contains("media=\"(max-width: 767px)\""));
		Assert.assertTrue(
			markup, markup.contains("media=\"(min-width: 768px)\""));

		// The img inside carries the last breakpoint, serving both as the
		// fallback and as the source used when no media condition matches.

		Assert.assertTrue(markup, markup.contains("sizes=\"50vw\""));
	}

	private BreakpointDefinition _breakpointDefinition(
		String mediaQuery, String sizes) {

		return new BreakpointDefinition(
			false, "test", null, mediaQuery, sizes,
			Collections.<String, String>emptyMap());
	}

	private void _givenPreset(BreakpointDefinition... breakpointDefinitions) {
		Mockito.when(
			_presetDefinitionResolver.resolve(
				Mockito.anyLong(), Mockito.anyLong(),
				Mockito.nullable(String.class))
		).thenReturn(
			new PresetDefinition(
				null, null, "test", Arrays.asList(breakpointDefinitions))
		);
	}

	private void _givenResponsiveImage(
			ResponsiveImageBreakpoint... responsiveImageBreakpoints)
		throws Exception {

		List<ResponsiveImageBreakpoint> list = Arrays.asList(
			responsiveImageBreakpoints);

		Mockito.when(
			_responsiveImageProvider.getResponsiveImage(
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			new ResponsiveImage(list, "/documents/1/2/photo.jpg")
		);
	}

	private ResponsiveImageRequest _request() {
		return ResponsiveImageRequestBuilder.imageResource(
			_imageResource
		).build();
	}

	private ResponsiveImageBreakpointVariant _variant(String url, int width) {
		return ResponsiveImageBreakpointVariantBuilder.url(
			url
		).width(
			width
		).build();
	}

	private static final String _ORIGINAL_IMG_TAG =
		"<img alt=\"A photo\" src=\"/documents/1/2/photo.jpg\" />";

	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);
	private final PresetDefinitionResolver _presetDefinitionResolver =
		Mockito.mock(PresetDefinitionResolver.class);
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);
	private final ResponsiveImageMarkupRendererImpl
		_responsiveImageMarkupRendererImpl =
			new ResponsiveImageMarkupRendererImpl();
	private final ResponsiveImageProvider _responsiveImageProvider =
		Mockito.mock(ResponsiveImageProvider.class);
	private final ResponsiveImageProviderSelector
		_responsiveImageProviderSelector = Mockito.mock(
			ResponsiveImageProviderSelector.class);

}