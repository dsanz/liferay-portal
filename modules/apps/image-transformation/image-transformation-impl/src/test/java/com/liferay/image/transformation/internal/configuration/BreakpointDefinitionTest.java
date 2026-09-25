/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Sanz
 */
public class BreakpointDefinitionTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testAutoSizesIsIgnoredWhenNotLazy() {

		// A browser only honors sizes="auto" on a lazily loaded image, so
		// emitting it here would be an invalid attribute rather than a hint.

		BreakpointDefinition breakpointDefinition =
			_autoSizesBreakpointDefinition();

		Assert.assertEquals("100vw", breakpointDefinition.getSizes(false));
	}

	@Test
	public void testAutoSizesKeepsDeclaredSizesAsFallback() {

		// The declared value stays behind the keyword so that a browser
		// without automatic sizing still receives a real one.

		BreakpointDefinition breakpointDefinition =
			_autoSizesBreakpointDefinition();

		Assert.assertEquals("auto, 100vw", breakpointDefinition.getSizes(true));
	}

	@Test
	public void testSizesAreUntouchedWithoutAutoSizes() {

		// Automatic sizing is an opt in. A breakpoint that did not ask for it
		// must render the declared value whether or not the image is lazy.

		BreakpointDefinition breakpointDefinition = new BreakpointDefinition(
			false, "default", null, null, "100vw",
			Collections.<String, String>emptyMap());

		Assert.assertEquals("100vw", breakpointDefinition.getSizes(true));
		Assert.assertEquals("100vw", breakpointDefinition.getSizes(false));
	}

	private BreakpointDefinition _autoSizesBreakpointDefinition() {
		return new BreakpointDefinition(
			true, "default", null, null, "100vw",
			Collections.<String, String>emptyMap());
	}

}