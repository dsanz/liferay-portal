/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.cdn;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetGroup;
import com.liferay.image.transformation.ImagePresetResolver;
import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ImageVariant;
import com.liferay.image.transformation.ImageVariantGroup;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * Wires the provider to a mocked servlet request and URL builder, so that what
 * is asserted is the markup a browser would actually receive.
 *
 * @author Daniel Sanz
 */
public class CDNImageTransformationProviderTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		Mockito.when(
			_imageResource.getURL()
		).thenReturn(
			"/documents/1/2/photo.jpg"
		);

		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			"image/jpeg"
		);

		Mockito.when(
			_imageTransformationConfigurationHelper.getCompanyId(
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			_COMPANY_ID
		);

		Mockito.when(
			_imageTransformationConfigurationHelper.
				getImageTransformationConfiguration(_COMPANY_ID)
		).thenReturn(
			_imageTransformationConfiguration
		);

		Mockito.when(
			_imageTransformationConfiguration.defaultTransformations()
		).thenReturn(
			new String[] {"disable=upscale"}
		);

		Mockito.when(
			_imageTransformationConfiguration.rendererName()
		).thenReturn(
			"fastly"
		);

		Mockito.when(
			_imageTransformationConfiguration.variantWidths()
		).thenReturn(
			new String[] {"320", "640", "1280"}
		);

		_setUpAbsolutePortalURLBuilderFactory(true);

		ReflectionTestUtil.setFieldValue(
			_cdnImageTransformationProvider, "_absolutePortalURLBuilderFactory",
			_absolutePortalURLBuilderFactory);
		ReflectionTestUtil.setFieldValue(
			_cdnImageTransformationProvider, "_imagePresetResolver",
			_imagePresetResolver);
		ReflectionTestUtil.setFieldValue(
			_cdnImageTransformationProvider,
			"_imageTransformationConfigurationHelper",
			_imageTransformationConfigurationHelper);
	}

	@Test
	public void testConfiguredRendererNameReachesTheURLBuilder() {

		// Which vendor spells the URLs is a configuration decision, so the
		// builder must be told rather than left to pick by service ranking.

		_givenPresetGroup(_preset(null, null, null, "100vw"));

		_firstGroupVariants();

		Assert.assertEquals(
			_rendererNames.toString(), Collections.singleton("fastly"),
			_rendererNames);
	}

	@Test
	public void testDeclinesImagesTheCDNDoesNotFront() {

		// An absolute URL on another host never reaches the optimizer, so
		// appending parameters would change the URL without changing the
		// response.

		Mockito.when(
			_imageResource.getURL()
		).thenReturn(
			"https://example.com/photo.jpg"
		);

		Assert.assertFalse(
			_cdnImageTransformationProvider.isTransformable(_imageResource));
	}

	@Test
	public void testDeclinesSVG() {
		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			"image/svg+xml"
		);

		Assert.assertFalse(
			_cdnImageTransformationProvider.isTransformable(_imageResource));
	}

	@Test
	public void testDeclinesUnknownMimeType() {
		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			null
		);

		Assert.assertFalse(
			_cdnImageTransformationProvider.isTransformable(_imageResource));
	}

	@Test
	public void testMaxWidthTruncatesLadderKeepingTheBoundaryWidth() {

		// A soft cap: 640 is above the 500 pixel maximum but is the smallest
		// candidate that satisfies it, so it must survive. Filtering strictly
		// below would leave only 320.

		_givenPresetGroup(_preset(null, 500, null, "500px"));

		List<ImageVariant> imageVariants = _firstGroupVariants();

		Assert.assertEquals(imageVariants.toString(), 2, imageVariants.size());
		Assert.assertEquals(Integer.valueOf(320), _widthOf(imageVariants, 0));
		Assert.assertEquals(Integer.valueOf(640), _widthOf(imageVariants, 1));
	}

	@Test
	public void testPassesThroughWhenNothingWasActuallyTransformed() {

		// No renderer bound, so every width builds the same URL. Emitting them
		// would advertise one image at three different widths.

		_setUpAbsolutePortalURLBuilderFactory(false);

		_givenPresetGroup(_preset(null, null, null, "100vw"));

		ResponsiveImage responsiveImage =
			_cdnImageTransformationProvider.getResponsiveImage(_request());

		Assert.assertTrue(
			String.valueOf(responsiveImage.getVariantGroups()),
			responsiveImage.getVariantGroups(
			).isEmpty());
		Assert.assertEquals(
			"/documents/1/2/photo.jpg", responsiveImage.getSrc());
	}

	@Test
	public void testRendersImgForASingleGroup() {
		_givenPresetGroup(_preset(null, null, null, "100vw"));

		String markup = _cdnImageTransformationProvider.render(
			"<img alt=\"A photo\" src=\"/documents/1/2/photo.jpg\" />",
			_request());

		Assert.assertTrue(markup, markup.startsWith("<img "));
		Assert.assertFalse(markup, markup.contains("<picture>"));
		Assert.assertTrue(markup, markup.contains("sizes=\"100vw\""));
		Assert.assertFalse(markup, markup.contains("loading="));
		Assert.assertTrue(markup, markup.contains("alt=\"A photo\""));

		Assert.assertTrue(
			markup,
			markup.contains(
				"srcset=\"/documents/1/2/photo.jpg?disable=upscale&amp;" +
					"width=320 320w, "));
	}

	@Test
	public void testRendersPictureForArtDirection() {

		// Two groups mean the crop differs by viewport, which srcset alone
		// cannot express.

		_givenPresetGroup(
			_preset("narrow", null, "(max-width: 767px)", "100vw", "crop=1:1"),
			_preset("wide", null, "(min-width: 768px)", "50vw", "crop=16:9"));

		String markup = _cdnImageTransformationProvider.render(
			"<img src=\"/documents/1/2/photo.jpg\" />", _request());

		Assert.assertTrue(markup, markup.startsWith("<picture>"));
		Assert.assertTrue(markup, markup.endsWith("</picture>"));
		Assert.assertTrue(
			markup, markup.contains("media=\"(max-width: 767px)\""));
		Assert.assertTrue(
			markup, markup.contains("media=\"(min-width: 768px)\""));
		Assert.assertTrue(markup, markup.contains("crop=1%3A1"));
		Assert.assertTrue(markup, markup.contains("crop=16%3A9"));

		// The img inside carries the last group, serving both as the fallback
		// and as the source used when no media condition matches.

		Assert.assertTrue(markup, markup.contains("sizes=\"50vw\""));
	}

	@Test
	public void testTransformsWithoutAHttpServletRequest() {

		// The content transformer chain has no request to give, and refusing
		// to transform there would leave web content unoptimized.

		_givenPresetGroup(_preset(null, null, null, "100vw"));

		ResponsiveImage responsiveImage =
			_cdnImageTransformationProvider.getResponsiveImage(
				_requestWithoutHttpServletRequest());

		Assert.assertFalse(
			String.valueOf(responsiveImage.getVariantGroups()),
			responsiveImage.getVariantGroups(
			).isEmpty());
	}

	@Test
	public void testVariantsCarryPresetTransformationsAndWidth() {
		_givenPresetGroup(
			_preset("narrow", null, "(max-width: 767px)", "100vw", "crop=1:1"));

		List<ImageVariant> imageVariants = _firstGroupVariants();

		Assert.assertEquals(imageVariants.toString(), 3, imageVariants.size());

		ImageVariant imageVariant = imageVariants.get(0);

		Assert.assertEquals(Integer.valueOf(320), imageVariant.getWidth());
		Assert.assertEquals(
			"/documents/1/2/photo.jpg?crop=1%3A1&disable=upscale&width=320",
			imageVariant.getURL());
	}

	private List<ImageVariant> _firstGroupVariants() {
		ResponsiveImage responsiveImage =
			_cdnImageTransformationProvider.getResponsiveImage(_request());

		List<ImageVariantGroup> imageVariantGroups =
			responsiveImage.getVariantGroups();

		ImageVariantGroup imageVariantGroup = imageVariantGroups.get(0);

		return imageVariantGroup.getVariants();
	}

	private void _givenPresetGroup(ImagePreset... imagePresets) {
		Mockito.when(
			_imagePresetResolver.resolve(
				Mockito.anyLong(), Mockito.nullable(String.class))
		).thenReturn(
			new ImagePresetGroup(
				null, null, "test", Arrays.asList(imagePresets))
		);
	}

	private ImagePreset _preset(
		String breakpointName, Integer maxWidth, String mediaQuery,
		String sizes) {

		return new ImagePreset(
			false, breakpointName, maxWidth, mediaQuery, sizes,
			Collections.<String, String>emptyMap());
	}

	private ImagePreset _preset(
		String breakpointName, Integer maxWidth, String mediaQuery,
		String sizes, String transformation) {

		int i = transformation.indexOf('=');

		return new ImagePreset(
			false, breakpointName, maxWidth, mediaQuery, sizes,
			HashMapBuilder.put(
				transformation.substring(0, i), transformation.substring(i + 1)
			).build());
	}

	private ResponsiveImageRequest _request() {
		return ResponsiveImageRequest.builder(
			_imageResource
		).httpServletRequest(
			_httpServletRequest
		).build();
	}

	private ResponsiveImageRequest _requestWithoutHttpServletRequest() {
		return ResponsiveImageRequest.of(_imageResource);
	}

	/**
	 * Stands in for the real builder, which lives in a package this module
	 * cannot see. When <code>transforming</code> is false it returns the URL
	 * untouched, reproducing what happens when no vendor renderer is deployed.
	 */
	private void _setUpAbsolutePortalURLBuilderFactory(boolean transforming) {
		Mockito.when(
			_absolutePortalURLBuilderFactory.getAbsolutePortalURLBuilder(
				Mockito.nullable(HttpServletRequest.class))
		).thenReturn(
			_absolutePortalURLBuilder
		);

		Mockito.when(
			_absolutePortalURLBuilder.forTransformedImage(Mockito.anyString())
		).thenAnswer(
			invocation -> {
				String url = invocation.getArgument(0);

				Map<String, String> transformations = new TreeMap<>();

				TransformedImageAbsolutePortalURLBuilder
					transformedImageAbsolutePortalURLBuilder = Mockito.mock(
						TransformedImageAbsolutePortalURLBuilder.class);

				Mockito.when(
					transformedImageAbsolutePortalURLBuilder.rendererName(
						Mockito.nullable(String.class))
				).thenAnswer(
					rendererNameInvocation -> {
						_rendererNames.add(
							rendererNameInvocation.getArgument(0));

						return transformedImageAbsolutePortalURLBuilder;
					}
				);

				Mockito.when(
					transformedImageAbsolutePortalURLBuilder.param(
						Mockito.anyString(), Mockito.anyString())
				).thenAnswer(
					paramInvocation -> {
						transformations.put(
							paramInvocation.getArgument(0),
							paramInvocation.getArgument(1));

						return transformedImageAbsolutePortalURLBuilder;
					}
				);

				Mockito.when(
					transformedImageAbsolutePortalURLBuilder.build()
				).thenAnswer(
					buildInvocation -> {
						if (!transforming || transformations.isEmpty()) {
							return url;
						}

						StringBuilder sb = new StringBuilder(url);

						sb.append('?');

						for (Map.Entry<String, String> entry :
								transformations.entrySet()) {

							if (sb.charAt(sb.length() - 1) != '?') {
								sb.append('&');
							}

							sb.append(entry.getKey());
							sb.append('=');
							sb.append(
								entry.getValue(
								).replace(
									":", "%3A"
								));
						}

						return sb.toString();
					}
				);

				return transformedImageAbsolutePortalURLBuilder;
			}
		);
	}

	private Integer _widthOf(List<ImageVariant> imageVariants, int index) {
		ImageVariant imageVariant = imageVariants.get(index);

		return imageVariant.getWidth();
	}

	private static final long _COMPANY_ID = 42L;

	private final AbsolutePortalURLBuilder _absolutePortalURLBuilder =
		Mockito.mock(AbsolutePortalURLBuilder.class);
	private final AbsolutePortalURLBuilderFactory
		_absolutePortalURLBuilderFactory = Mockito.mock(
			AbsolutePortalURLBuilderFactory.class);
	private final CDNImageTransformationProvider
		_cdnImageTransformationProvider = new CDNImageTransformationProvider();
	private final HttpServletRequest _httpServletRequest = Mockito.mock(
		HttpServletRequest.class);
	private final ImagePresetResolver _imagePresetResolver = Mockito.mock(
		ImagePresetResolver.class);
	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);
	private final ImageTransformationConfiguration
		_imageTransformationConfiguration = Mockito.mock(
			ImageTransformationConfiguration.class);
	private final ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper = Mockito.mock(
			ImageTransformationConfigurationHelper.class);
	private final Set<String> _rendererNames = new HashSet<>();

}