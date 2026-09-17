/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImageBreakpoint;
import com.liferay.image.transformation.ImageBreakpointVariant;
import com.liferay.image.transformation.ImageBreakpointVariantBuilder;
import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.ResponsiveImageRequestBuilder;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.image.transformation.preset.BreakpointPreset;
import com.liferay.image.transformation.preset.ImagePreset;
import com.liferay.image.transformation.preset.ImagePresetResolver;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

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
			_imageTransformationProviderSelector.getImageTransformationProvider(
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			_imageTransformationProvider
		);

		// Null means "I do not render my own markup", which is what every
		// provider but Adaptive Media says.

		Mockito.when(
			_imageTransformationProvider.render(
				Mockito.anyString(), Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			null
		);

		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl, "_imagePresetResolver",
			_imagePresetResolver);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl,
			"_imageTransformationConfigurationHelper",
			_imageTransformationConfigurationHelper);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl,
			"_imageTransformationProviderSelector",
			_imageTransformationProviderSelector);
	}

	@Test
	public void testPassesThroughWhenNothingWasTransformed() throws Exception {

		// Every width built the same URL, so no renderer is bound. Emitting the
		// descriptors would have the browser trust them and pick wrong.

		_givenPreset(_breakpointPreset(null, "100vw"));
		_givenResponsiveImage(
			ImageBreakpoint.of(
				null, "100vw",
				Arrays.asList(
					_variant("/documents/1/2/photo.jpg", 320),
					_variant("/documents/1/2/photo.jpg", 640))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertEquals(_ORIGINAL_IMG_TAG, markup);
	}

	@Test
	public void testPassesThroughWhenTheProviderRendersItsOwnMarkup()
		throws Exception {

		Mockito.when(
			_imageTransformationProvider.render(
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
		_givenPreset(_breakpointPreset(null, "100vw"));
		_givenResponsiveImage(
			ImageBreakpoint.of(
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
			_breakpointPreset("(max-width: 767px)", "100vw"),
			_breakpointPreset("(min-width: 768px)", "50vw"));
		_givenResponsiveImage(
			ImageBreakpoint.of(
				"(max-width: 767px)", "100vw",
				Collections.singletonList(
					_variant("/documents/1/2/photo.jpg?crop=1%3A1", 320))),
			ImageBreakpoint.of(
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

	private BreakpointPreset _breakpointPreset(
		String mediaQuery, String sizes) {

		return new BreakpointPreset(
			false, "test", null, mediaQuery, sizes,
			Collections.<String, String>emptyMap());
	}

	private void _givenPreset(BreakpointPreset... breakpointPresets) {
		Mockito.when(
			_imagePresetResolver.resolve(
				Mockito.anyLong(), Mockito.anyLong(),
				Mockito.nullable(String.class))
		).thenReturn(
			new ImagePreset(
				null, null, "test", Arrays.asList(breakpointPresets))
		);
	}

	private void _givenResponsiveImage(ImageBreakpoint... imageBreakpoints)
		throws Exception {

		List<ImageBreakpoint> list = Arrays.asList(imageBreakpoints);

		Mockito.when(
			_imageTransformationProvider.getResponsiveImage(
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

	private ImageBreakpointVariant _variant(String url, int width) {
		return ImageBreakpointVariantBuilder.url(
			url
		).width(
			width
		).build();
	}

	private static final String _ORIGINAL_IMG_TAG =
		"<img alt=\"A photo\" src=\"/documents/1/2/photo.jpg\" />";

	private final ImagePresetResolver _imagePresetResolver = Mockito.mock(
		ImagePresetResolver.class);
	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);
	private final ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper = Mockito.mock(
			ImageTransformationConfigurationHelper.class);
	private final ImageTransformationProvider _imageTransformationProvider =
		Mockito.mock(ImageTransformationProvider.class);
	private final ImageTransformationProviderSelector
		_imageTransformationProviderSelector = Mockito.mock(
			ImageTransformationProviderSelector.class);
	private final ResponsiveImageMarkupRendererImpl
		_responsiveImageMarkupRendererImpl =
			new ResponsiveImageMarkupRendererImpl();

}