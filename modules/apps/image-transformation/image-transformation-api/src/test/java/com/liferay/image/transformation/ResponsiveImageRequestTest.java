/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Daniel Sanz
 */
public class ResponsiveImageRequestTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test(expected = IllegalArgumentException.class)
	public void testBuilderRejectsMissingImageResource() {
		ResponsiveImageRequestBuilder.imageResource(null);
	}

	@Test
	public void testBuildsWithoutAHttpServletRequest() {

		// Content transformers and template transformer listeners take a
		// string and return a string, and no portal wide thread local carries
		// the current request. Requiring one would leave those paths unable to
		// ask for a transformed image at all.

		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.of(_imageResource);

		Assert.assertNull(responsiveImageRequest.getHttpServletRequest());
		Assert.assertSame(
			_imageResource, responsiveImageRequest.getImageResource());
	}

	@Test
	public void testCarriesAHttpServletRequestWhenGiven() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequestBuilder.imageResource(
				_imageResource
			).httpServletRequest(
				_httpServletRequest
			).build();

		Assert.assertSame(
			_httpServletRequest,
			responsiveImageRequest.getHttpServletRequest());
	}

	@Test
	public void testDefersLazinessToConfigurationByDefault() {

		// Null rather than true: defaulting every image to lazy would lazily
		// load the largest contentful one, which costs a Core Web Vital. The
		// placement decides unless this caller says otherwise.

		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.of(_imageResource);

		Assert.assertNull(responsiveImageRequest.getLazy());
		Assert.assertNull(responsiveImageRequest.getPresetName());
	}

	@Test
	public void testDefersToTheCompanyWhenNoSiteIsGiven() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequestBuilder.imageResource(
				_imageResource
			).build();

		// Zero rather than the resource's site. The site an image is stored in
		// is a different question from the site it is rendered on, and group
		// scoped configuration cascades to the company anyway.

		Assert.assertEquals(0, responsiveImageRequest.getGroupId());
	}

	@Test
	public void testEagerRequestCarriesLazyFalse() {

		// sizes="auto" is only valid alongside loading="lazy", so this is the
		// flag that decides whether the automatic strategy may be used at all.

		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequestBuilder.imageResource(
				_imageResource
			).lazy(
				false
			).build();

		Assert.assertEquals(Boolean.FALSE, responsiveImageRequest.getLazy());
	}

	@Test
	public void testPresetNameIsCarried() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequestBuilder.imageResource(
				_imageResource
			).presetName(
				"card"
			).build();

		Assert.assertEquals("card", responsiveImageRequest.getPresetName());
	}

	@Test
	public void testSiteIsCarried() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequestBuilder.imageResource(
				_imageResource
			).groupId(
				12345
			).build();

		Assert.assertEquals(12345, responsiveImageRequest.getGroupId());
	}

	private final HttpServletRequest _httpServletRequest = Mockito.mock(
		HttpServletRequest.class);
	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);

}