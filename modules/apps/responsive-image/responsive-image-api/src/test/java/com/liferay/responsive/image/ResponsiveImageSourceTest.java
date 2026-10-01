/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Sanz
 */
public class ResponsiveImageSourceTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test(expected = UnsupportedOperationException.class)
	public void testGetCandidatesReturnsAnUnmodifiableList() {
		ResponsiveImageSource responsiveImageSource =
			ResponsiveImageSource.builder(
			).candidates(
				_candidates
			).mediaQuery(
				null
			).sizes(
				"100vw"
			).build();

		List<ResponsiveImageCandidate> responsiveImageCandidates =
			responsiveImageSource.getCandidates();

		responsiveImageCandidates.add(
			ResponsiveImageCandidate.builder(
				"/other.jpg"
			).build());
	}

	@Test
	public void testGetMediaQueryWhenUnconditional() {
		ResponsiveImageSource responsiveImageSource =
			ResponsiveImageSource.builder(
			).candidates(
				Collections.<ResponsiveImageCandidate>emptyList()
			).mediaQuery(
				null
			).sizes(
				"100vw"
			).build();

		Assert.assertNull(responsiveImageSource.getMediaQuery());
	}

	@Test
	public void testGetSizesWhenNoneIsGiven() {
		ResponsiveImageSource responsiveImageSource =
			ResponsiveImageSource.builder(
			).candidates(
				_candidates
			).mediaQuery(
				"(max-width: 640px)"
			).sizes(
				null
			).build();

		Assert.assertEquals(
			"(max-width: 640px)", responsiveImageSource.getMediaQuery());
		Assert.assertNull(responsiveImageSource.getSizes());
	}

	private final List<ResponsiveImageCandidate> _candidates = Arrays.asList(
		ResponsiveImageCandidate.builder(
			"/photo.jpg?width=320"
		).width(
			320
		).build(),
		ResponsiveImageCandidate.builder(
			"/photo.jpg?width=640"
		).width(
			640
		).build());

}