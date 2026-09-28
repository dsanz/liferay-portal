/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

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
	public void testDefersToTheCompanyWhenNoSiteIsGiven() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequestBuilder.imageResource(
				_imageResource
			).build();

		Assert.assertEquals(0, responsiveImageRequest.getGroupId());
	}

	@Test
	public void testNamesNoPresetByDefault() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.of(_imageResource);

		Assert.assertNull(responsiveImageRequest.getPresetName());
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