/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.cdn;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageBreakpoint;
import com.liferay.responsive.image.ResponsiveImageBreakpointVariant;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageRequestBuilder;
import com.liferay.responsive.image.internal.configuration.BreakpointDefinition;
import com.liferay.responsive.image.internal.configuration.PresetDefinition;
import com.liferay.responsive.image.internal.configuration.PresetDefinitionResolver;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfiguration;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;

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
public class CDNResponsiveImageProviderTest {

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
			_responsiveImageConfigurationHelper.getCompanyId(
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			_COMPANY_ID
		);

		Mockito.when(
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				0, _COMPANY_ID)
		).thenReturn(
			_responsiveImageConfiguration
		);

		Mockito.when(
			_responsiveImageConfiguration.defaultTransformations()
		).thenReturn(
			new String[] {"disable=upscale"}
		);

		Mockito.when(
			_responsiveImageConfiguration.urlRendererName()
		).thenReturn(
			"fastly"
		);

		Mockito.when(
			_responsiveImageConfiguration.variantWidths()
		).thenReturn(
			new String[] {"320", "640", "1280"}
		);

		_setUpAbsolutePortalURLBuilderFactory(true);

		ReflectionTestUtil.setFieldValue(
			_cdnResponsiveImageProvider, "_absolutePortalURLBuilderFactory",
			_absolutePortalURLBuilderFactory);
		Mockito.when(
			_imageTransformationURLRenderer.getName()
		).thenReturn(
			"fastly"
		);

		Mockito.when(
			_serviceTrackerList.iterator()
		).thenAnswer(
			invocation -> {
				List<ImageTransformationURLRenderer> renderers =
					Collections.singletonList(_imageTransformationURLRenderer);

				return renderers.iterator();
			}
		);

		ReflectionTestUtil.setFieldValue(
			_cdnResponsiveImageProvider, "_serviceTrackerList",
			_serviceTrackerList);
		ReflectionTestUtil.setFieldValue(
			_cdnResponsiveImageProvider, "_presetDefinitionResolver",
			_presetDefinitionResolver);
		ReflectionTestUtil.setFieldValue(
			_cdnResponsiveImageProvider, "_responsiveImageConfigurationHelper",
			_responsiveImageConfigurationHelper);
	}

	@Test
	public void testConfiguredRendererNameReachesTheURLBuilder() {

		// Which vendor spells the URLs is a configuration decision, so this
		// provider resolves the configured name against the registered
		// renderers and hands the builder the one it found, rather than
		// leaving the builder to pick by service ranking.

		_givenPresetGroup(_preset(null, null, null, "100vw"));

		_firstGroupVariants();

		Assert.assertEquals(
			_imageTransformationURLRenderers.toString(),
			Collections.singleton(_imageTransformationURLRenderer),
			_imageTransformationURLRenderers);
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
			_cdnResponsiveImageProvider.canTransform(_imageResource));
	}

	@Test
	public void testDeclinesSVG() {
		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			"image/svg+xml"
		);

		Assert.assertFalse(
			_cdnResponsiveImageProvider.canTransform(_imageResource));
	}

	@Test
	public void testDeclinesUnknownMimeType() {
		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			null
		);

		Assert.assertFalse(
			_cdnResponsiveImageProvider.canTransform(_imageResource));
	}

	@Test
	public void testMaxWidthTruncatesLadderKeepingTheBoundaryWidth() {

		// A soft cap: 640 is above the 500 pixel maximum but is the smallest
		// candidate that satisfies it, so it must survive. Filtering strictly
		// below would leave only 320.

		_givenPresetGroup(_preset(null, 500, null, "500px"));

		List<ResponsiveImageBreakpointVariant>
			responsiveImageBreakpointVariants = _firstGroupVariants();

		Assert.assertEquals(
			responsiveImageBreakpointVariants.toString(), 2,
			responsiveImageBreakpointVariants.size());
		Assert.assertEquals(
			Integer.valueOf(320),
			_widthOf(responsiveImageBreakpointVariants, 0));
		Assert.assertEquals(
			Integer.valueOf(640),
			_widthOf(responsiveImageBreakpointVariants, 1));
	}

	@Test
	public void testTransformsWithoutAHttpServletRequest() {

		// The content transformer chain has no request to give, and refusing
		// to transform there would leave web content unoptimized.

		_givenPresetGroup(_preset(null, null, null, "100vw"));

		ResponsiveImage responsiveImage =
			_cdnResponsiveImageProvider.getResponsiveImage(
				_requestWithoutHttpServletRequest());

		Assert.assertFalse(
			String.valueOf(responsiveImage.getBreakpoints()),
			responsiveImage.getBreakpoints(
			).isEmpty());
	}

	@Test
	public void testVariantsCarryPresetTransformationsAndWidth() {
		_givenPresetGroup(
			_preset("narrow", null, "(max-width: 767px)", "100vw", "crop=1:1"));

		List<ResponsiveImageBreakpointVariant>
			responsiveImageBreakpointVariants = _firstGroupVariants();

		Assert.assertEquals(
			responsiveImageBreakpointVariants.toString(), 3,
			responsiveImageBreakpointVariants.size());

		ResponsiveImageBreakpointVariant responsiveImageBreakpointVariant =
			responsiveImageBreakpointVariants.get(0);

		Assert.assertEquals(
			Integer.valueOf(320), responsiveImageBreakpointVariant.getWidth());
		Assert.assertEquals(
			"/documents/1/2/photo.jpg?crop=1%3A1&disable=upscale&width=320",
			responsiveImageBreakpointVariant.getURL());
	}

	private List<ResponsiveImageBreakpointVariant> _firstGroupVariants() {
		ResponsiveImage responsiveImage =
			_cdnResponsiveImageProvider.getResponsiveImage(_request());

		List<ResponsiveImageBreakpoint> responsiveImageBreakpoints =
			responsiveImage.getBreakpoints();

		ResponsiveImageBreakpoint responsiveImageBreakpoint =
			responsiveImageBreakpoints.get(0);

		return responsiveImageBreakpoint.getVariants();
	}

	private void _givenPresetGroup(
		BreakpointDefinition... breakpointDefinitions) {

		Mockito.when(
			_presetDefinitionResolver.resolve(
				Mockito.anyLong(), Mockito.anyLong(),
				Mockito.nullable(String.class))
		).thenReturn(
			new PresetDefinition(
				null, null, "test", Arrays.asList(breakpointDefinitions))
		);
	}

	private BreakpointDefinition _preset(
		String breakpointName, Integer maxWidth, String mediaQuery,
		String sizes) {

		return new BreakpointDefinition(
			false, breakpointName, maxWidth, mediaQuery, sizes,
			Collections.<String, String>emptyMap());
	}

	private BreakpointDefinition _preset(
		String breakpointName, Integer maxWidth, String mediaQuery,
		String sizes, String transformation) {

		int i = transformation.indexOf('=');

		return new BreakpointDefinition(
			false, breakpointName, maxWidth, mediaQuery, sizes,
			HashMapBuilder.put(
				transformation.substring(0, i), transformation.substring(i + 1)
			).build());
	}

	private ResponsiveImageRequest _request() {
		return ResponsiveImageRequestBuilder.imageResource(
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
					transformedImageAbsolutePortalURLBuilder.setRenderer(
						Mockito.nullable(ImageTransformationURLRenderer.class))
				).thenAnswer(
					rendererInvocation -> {
						_imageTransformationURLRenderers.add(
							rendererInvocation.getArgument(0));

						return transformedImageAbsolutePortalURLBuilder;
					}
				);

				Mockito.when(
					transformedImageAbsolutePortalURLBuilder.addTransformation(
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

	private Integer _widthOf(
		List<ResponsiveImageBreakpointVariant>
			responsiveImageBreakpointVariants,
		int index) {

		ResponsiveImageBreakpointVariant responsiveImageBreakpointVariant =
			responsiveImageBreakpointVariants.get(index);

		return responsiveImageBreakpointVariant.getWidth();
	}

	private static final long _COMPANY_ID = 42L;

	private final AbsolutePortalURLBuilder _absolutePortalURLBuilder =
		Mockito.mock(AbsolutePortalURLBuilder.class);
	private final AbsolutePortalURLBuilderFactory
		_absolutePortalURLBuilderFactory = Mockito.mock(
			AbsolutePortalURLBuilderFactory.class);
	private final CDNResponsiveImageProvider _cdnResponsiveImageProvider =
		new CDNResponsiveImageProvider();
	private final HttpServletRequest _httpServletRequest = Mockito.mock(
		HttpServletRequest.class);
	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);
	private final ImageTransformationURLRenderer
		_imageTransformationURLRenderer = Mockito.mock(
			ImageTransformationURLRenderer.class);
	private final Set<ImageTransformationURLRenderer>
		_imageTransformationURLRenderers = new HashSet<>();
	private final PresetDefinitionResolver _presetDefinitionResolver =
		Mockito.mock(PresetDefinitionResolver.class);
	private final ResponsiveImageConfiguration _responsiveImageConfiguration =
		Mockito.mock(ResponsiveImageConfiguration.class);
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);

	@SuppressWarnings("unchecked")
	private final ServiceTrackerList<ImageTransformationURLRenderer>
		_serviceTrackerList = Mockito.mock(ServiceTrackerList.class);

}