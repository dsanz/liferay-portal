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
import com.liferay.responsive.image.ResponsiveImageCandidate;
import com.liferay.responsive.image.ResponsiveImageCandidateBuilder;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageRequestBuilder;
import com.liferay.responsive.image.ResponsiveImageSource;
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

		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl,
			"_responsiveImageConfigurationHelper",
			_responsiveImageConfigurationHelper);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageMarkupRendererImpl, "_responsiveImageFactory",
			_responsiveImageFactory);
	}

	@Test
	public void testDeclinesWhenNothingWasTransformed() throws Exception {
		_givenResponsiveImage(
			false,
			ResponsiveImageSource.of(
				null, "100vw",
				Arrays.asList(
					_candidate("/documents/1/2/photo.jpg", 320),
					_candidate("/documents/1/2/photo.jpg", 640))));

		Assert.assertNull(
			_responsiveImageMarkupRendererImpl.render(
				_ORIGINAL_IMG_TAG, _request()));
	}

	@FeatureFlag(enable = false, value = "LPD-94784")
	@Test
	public void testDeclinesWhenTheFeatureFlagIsDisabled() throws Exception {
		_givenResponsiveImage(
			false,
			ResponsiveImageSource.of(
				null, "100vw",
				Arrays.asList(
					_candidate("/documents/1/2/photo.jpg?width=320", 320),
					_candidate("/documents/1/2/photo.jpg?width=640", 640))));

		Assert.assertNull(
			_responsiveImageMarkupRendererImpl.render(
				_ORIGINAL_IMG_TAG, _request()));

		Mockito.verifyNoInteractions(_responsiveImageFactory);
	}

	@Test
	public void testRendersImgForASingleSource() throws Exception {
		_givenResponsiveImage(
			false,
			ResponsiveImageSource.of(
				null, "100vw",
				Arrays.asList(
					_candidate("/documents/1/2/photo.jpg?width=320", 320),
					_candidate("/documents/1/2/photo.jpg?width=640", 640))));

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
	public void testRendersLoadingLazyFromTheModel() throws Exception {
		_givenResponsiveImage(
			true,
			ResponsiveImageSource.of(
				null, "100vw",
				Arrays.asList(
					_candidate("/documents/1/2/photo.jpg?width=320", 320),
					_candidate("/documents/1/2/photo.jpg?width=640", 640))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertTrue(markup, markup.contains("loading=\"lazy\""));
	}

	@Test
	public void testRendersPictureForArtDirection() throws Exception {
		_givenResponsiveImage(
			false,
			ResponsiveImageSource.of(
				"(max-width: 767px)", "100vw",
				Collections.singletonList(
					_candidate("/documents/1/2/photo.jpg?crop=1%3A1", 320))),
			ResponsiveImageSource.of(
				"(min-width: 768px)", "50vw",
				Collections.singletonList(
					_candidate("/documents/1/2/photo.jpg?crop=16%3A9", 960))));

		String markup = _responsiveImageMarkupRendererImpl.render(
			_ORIGINAL_IMG_TAG, _request());

		Assert.assertTrue(markup, markup.startsWith("<picture>"));
		Assert.assertTrue(markup, markup.endsWith("</picture>"));
		Assert.assertTrue(
			markup, markup.contains("media=\"(max-width: 767px)\""));
		Assert.assertTrue(
			markup, markup.contains("media=\"(min-width: 768px)\""));

		Assert.assertTrue(markup, markup.contains("sizes=\"50vw\""));
	}

	private ResponsiveImageCandidate _candidate(String url, int width) {
		return ResponsiveImageCandidateBuilder.url(
			url
		).width(
			width
		).build();
	}

	private void _givenResponsiveImage(
			boolean lazy, ResponsiveImageSource... responsiveImageSources)
		throws Exception {

		List<ResponsiveImageSource> list = Arrays.asList(
			responsiveImageSources);

		Mockito.when(
			_responsiveImageFactory.create(
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			new ResponsiveImage(lazy, list)
		);
	}

	private ResponsiveImageRequest _request() {
		return ResponsiveImageRequestBuilder.imageResource(
			_imageResource
		).build();
	}

	private static final String _ORIGINAL_IMG_TAG =
		"<img alt=\"A photo\" src=\"/documents/1/2/photo.jpg\" />";

	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);
	private final ResponsiveImageFactory _responsiveImageFactory = Mockito.mock(
		ResponsiveImageFactory.class);
	private final ResponsiveImageMarkupRendererImpl
		_responsiveImageMarkupRendererImpl =
			new ResponsiveImageMarkupRendererImpl();

}