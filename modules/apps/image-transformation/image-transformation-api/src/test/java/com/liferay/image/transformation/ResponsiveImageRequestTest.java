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
	public void testBuilderRejectsMissingHttpServletRequest() {

		// Without a request there is no CDN host and no proxy path, so any URL
		// built would be wrong for the deployment in a way nothing downstream
		// can detect. Failing here is the point.

		ResponsiveImageRequest.builder(_imageResource, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBuilderRejectsMissingImageResource() {
		ResponsiveImageRequest.builder(null, _httpServletRequest);
	}

	@Test
	public void testDefaultsToLazyWithNoPresetGroup() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.of(_imageResource, _httpServletRequest);

		Assert.assertTrue(responsiveImageRequest.isLazy());
		Assert.assertNull(responsiveImageRequest.getPresetGroupName());
		Assert.assertSame(
			_imageResource, responsiveImageRequest.getImageResource());
		Assert.assertSame(
			_httpServletRequest,
			responsiveImageRequest.getHttpServletRequest());
	}

	@Test
	public void testEagerRequestCarriesLazyFalse() {

		// sizes="auto" is only valid alongside loading="lazy", so this is the
		// flag that decides whether the automatic strategy may be used at all.

		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.builder(
				_imageResource, _httpServletRequest
			).lazy(
				false
			).build();

		Assert.assertFalse(responsiveImageRequest.isLazy());
	}

	@Test
	public void testPresetGroupNameIsCarried() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.builder(
				_imageResource, _httpServletRequest
			).presetGroupName(
				"card"
			).build();

		Assert.assertEquals(
			"card", responsiveImageRequest.getPresetGroupName());
	}

	private final HttpServletRequest _httpServletRequest = Mockito.mock(
		HttpServletRequest.class);
	private final ImageResource _imageResource = Mockito.mock(
		ImageResource.class);

}