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
	public void testCandidatesAreUnmodifiable() {
		ResponsiveImageSource responsiveImageSource = ResponsiveImageSource.of(
			null, "100vw", _candidates);

		List<ResponsiveImageCandidate> responsiveImageCandidates =
			responsiveImageSource.getCandidates();

		responsiveImageCandidates.add(
			ResponsiveImageCandidateBuilder.url(
				"/other.jpg"
			).build());
	}

	@Test
	public void testOfAllowsAProviderToDetermineItsOwnGroup() {

		// Adaptive Media never sees a preset: its media conditions come from
		// its own configuration entries, and a source of one candidate
		// has no sizes to disambiguate.

		ResponsiveImageSource responsiveImageSource = ResponsiveImageSource.of(
			"(max-width: 640px)", null, _candidates);

		Assert.assertEquals(
			"(max-width: 640px)", responsiveImageSource.getMediaQuery());
		Assert.assertNull(responsiveImageSource.getSizes());
	}

	@Test
	public void testUnconditionalGroupHasNoMediaQuery() {

		// One unconditional source is what renders as a plain img rather
		// than as a picture element.

		ResponsiveImageSource responsiveImageSource = ResponsiveImageSource.of(
			null, "100vw", Collections.<ResponsiveImageCandidate>emptyList());

		Assert.assertNull(responsiveImageSource.getMediaQuery());
	}

	private final List<ResponsiveImageCandidate> _candidates = Arrays.asList(
		ResponsiveImageCandidateBuilder.url(
			"/photo.jpg?width=320"
		).width(
			320
		).build(),
		ResponsiveImageCandidateBuilder.url(
			"/photo.jpg?width=640"
		).width(
			640
		).build());

}