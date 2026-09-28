/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageCandidate;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageRequestBuilder;
import com.liferay.responsive.image.ResponsiveImageSource;
import com.liferay.responsive.image.internal.configuration.PresetDefinition;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry.ScopedConfiguration;
import com.liferay.responsive.image.internal.configuration.SourceDefinition;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Daniel Sanz
 */
public class ResponsiveImageFactoryTest {

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
			_responsiveImageConfigurationRegistry.getScopedConfiguration(
				Mockito.anyLong(), Mockito.anyLong())
		).thenReturn(
			_scopedConfiguration
		);

		Mockito.when(
			_scopedConfiguration.getDefaultTransformations()
		).thenReturn(
			HashMapBuilder.put(
				"disable", "upscale"
			).build()
		);

		Mockito.when(
			_scopedConfiguration.getURLRendererName()
		).thenReturn(
			"fastly"
		);

		Mockito.when(
			_scopedConfiguration.getCandidateWidths()
		).thenReturn(
			new TreeSet<>(Arrays.asList(320, 640, 1280))
		);

		Mockito.when(
			_imageTransformationURLRenderer.getName()
		).thenReturn(
			"fastly"
		);

		_setUpAbsolutePortalURLBuilderFactory(true);

		_responsiveImageFactory = new ResponsiveImageFactory(
			_absolutePortalURLBuilderFactory,
			_responsiveImageConfigurationRegistry,
			_responsiveImageConfigurationHelper,
			() -> Collections.singletonList(_imageTransformationURLRenderer));
	}

	@Test
	public void testCandidatesCarryPresetTransformationsAndWidth() {
		_givenPresetGroup(
			_preset("narrow", null, "(max-width: 767px)", "100vw", "crop=1:1"));

		List<ResponsiveImageCandidate> responsiveImageCandidates =
			_firstGroupCandidates();

		Assert.assertEquals(
			responsiveImageCandidates.toString(), 3,
			responsiveImageCandidates.size());

		ResponsiveImageCandidate responsiveImageCandidate =
			responsiveImageCandidates.get(0);

		Assert.assertEquals(
			Integer.valueOf(320), responsiveImageCandidate.getWidth());
		Assert.assertEquals(
			"/documents/1/2/photo.jpg?crop=1%3A1&disable=upscale&width=320",
			responsiveImageCandidate.getURL());
	}

	@Test
	public void testConfiguredRendererNameReachesTheURLBuilder() {
		_givenPresetGroup(_preset(null, null, null, "100vw"));

		_firstGroupCandidates();

		Assert.assertEquals(
			_imageTransformationURLRenderers.toString(),
			Collections.singleton(_imageTransformationURLRenderer),
			_imageTransformationURLRenderers);
	}

	@Test
	public void testDeclinesBeforeBuildingWhenNoRendererIsDeployed()
		throws Exception {

		_givenPresetGroup(_preset(null, null, null, "100vw"));

		Mockito.when(
			_scopedConfiguration.getURLRendererName()
		).thenReturn(
			"not-deployed"
		);

		Assert.assertNull(_responsiveImageFactory.create(_request()));

		Mockito.verifyNoInteractions(_absolutePortalURLBuilderFactory);
	}

	@Test
	public void testDeclinesImagesTheCDNDoesNotFront() {
		Mockito.when(
			_imageResource.getURL()
		).thenReturn(
			"https://example.com/photo.jpg"
		);

		Assert.assertNull(_responsiveImageFactory.create(_request()));
	}

	@Test
	public void testDeclinesSVG() {
		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			"image/svg+xml"
		);

		Assert.assertNull(_responsiveImageFactory.create(_request()));
	}

	@Test
	public void testDeclinesUnknownMimeType() {
		Mockito.when(
			_imageResource.getMimeType()
		).thenReturn(
			null
		);

		Assert.assertNull(_responsiveImageFactory.create(_request()));
	}

	@Test
	public void testMaxWidthTruncatesLadderKeepingTheBoundaryWidth() {
		_givenPresetGroup(_preset(null, 500, null, "500px"));

		List<ResponsiveImageCandidate> responsiveImageCandidates =
			_firstGroupCandidates();

		Assert.assertEquals(
			responsiveImageCandidates.toString(), 2,
			responsiveImageCandidates.size());
		Assert.assertEquals(
			Integer.valueOf(320), _widthOf(responsiveImageCandidates, 0));
		Assert.assertEquals(
			Integer.valueOf(640), _widthOf(responsiveImageCandidates, 1));
	}

	@Test
	public void testTransformsWithoutAHttpServletRequest() {
		_givenPresetGroup(_preset(null, null, null, "100vw"));

		ResponsiveImage responsiveImage = _responsiveImageFactory.create(
			_requestWithoutHttpServletRequest());

		Assert.assertFalse(
			String.valueOf(responsiveImage.getSources()),
			responsiveImage.getSources(
			).isEmpty());
	}

	private List<ResponsiveImageCandidate> _firstGroupCandidates() {
		ResponsiveImage responsiveImage = _responsiveImageFactory.create(
			_request());

		List<ResponsiveImageSource> responsiveImageSources =
			responsiveImage.getSources();

		ResponsiveImageSource responsiveImageSource =
			responsiveImageSources.get(0);

		return responsiveImageSource.getCandidates();
	}

	private void _givenPresetGroup(SourceDefinition... sourceDefinitions) {
		Mockito.when(
			_scopedConfiguration.getPresetDefinition(
				Mockito.nullable(String.class))
		).thenReturn(
			new PresetDefinition(
				null, null, "test", Arrays.asList(sourceDefinitions))
		);
	}

	private SourceDefinition _preset(
		String mediaConditionName, Integer maxWidth, String mediaQuery,
		String sizes) {

		return new SourceDefinition(
			false, mediaConditionName, maxWidth, mediaQuery, sizes,
			Collections.<String, String>emptyMap());
	}

	private SourceDefinition _preset(
		String mediaConditionName, Integer maxWidth, String mediaQuery,
		String sizes, String transformation) {

		int i = transformation.indexOf('=');

		return new SourceDefinition(
			false, mediaConditionName, maxWidth, mediaQuery, sizes,
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
		List<ResponsiveImageCandidate> responsiveImageCandidates, int index) {

		ResponsiveImageCandidate responsiveImageCandidate =
			responsiveImageCandidates.get(index);

		return responsiveImageCandidate.getWidth();
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private final AbsolutePortalURLBuilder _absolutePortalURLBuilder =
		Mockito.mock(AbsolutePortalURLBuilder.class);
	private final AbsolutePortalURLBuilderFactory
		_absolutePortalURLBuilderFactory = Mockito.mock(
			AbsolutePortalURLBuilderFactory.class);
	private final HttpServletRequest _httpServletRequest = Mockito.mock(
		HttpServletRequest.class);
	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);
	private final ImageTransformationURLRenderer
		_imageTransformationURLRenderer = Mockito.mock(
			ImageTransformationURLRenderer.class);
	private final Set<ImageTransformationURLRenderer>
		_imageTransformationURLRenderers = new HashSet<>();
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper = Mockito.mock(
			ResponsiveImageConfigurationHelper.class);
	private final ResponsiveImageConfigurationRegistry
		_responsiveImageConfigurationRegistry = Mockito.mock(
			ResponsiveImageConfigurationRegistry.class);
	private ResponsiveImageFactory _responsiveImageFactory;
	private final ScopedConfiguration _scopedConfiguration = Mockito.mock(
		ScopedConfiguration.class);

}