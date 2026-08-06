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
		ResponsiveImageRequest.builder(null);
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
			ResponsiveImageRequest.builder(
				_imageResource
			).httpServletRequest(
				_httpServletRequest
			).build();

		Assert.assertSame(
			_httpServletRequest,
			responsiveImageRequest.getHttpServletRequest());
	}

	@Test
	public void testDefaultsToLazyWithNoPresetGroup() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.of(_imageResource);

		Assert.assertTrue(responsiveImageRequest.isLazy());
		Assert.assertNull(responsiveImageRequest.getPresetGroupName());
	}

	@Test
	public void testEagerRequestCarriesLazyFalse() {

		// sizes="auto" is only valid alongside loading="lazy", so this is the
		// flag that decides whether the automatic strategy may be used at all.

		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.builder(
				_imageResource
			).lazy(
				false
			).build();

		Assert.assertFalse(responsiveImageRequest.isLazy());
	}

	@Test
	public void testPresetGroupNameIsCarried() {
		ResponsiveImageRequest responsiveImageRequest =
			ResponsiveImageRequest.builder(
				_imageResource
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