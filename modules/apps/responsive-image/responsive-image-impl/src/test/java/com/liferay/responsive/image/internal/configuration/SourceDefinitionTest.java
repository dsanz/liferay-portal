/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Sanz
 */
public class SourceDefinitionTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testAutoSizesIsIgnoredWhenNotLazy() {

		// A browser only honors sizes="auto" on a lazily loaded image, so
		// emitting it here would be an invalid attribute rather than a hint.

		SourceDefinition sourceDefinition = _autoSizesSourceDefinition();

		Assert.assertEquals("100vw", sourceDefinition.getSizes(false));
	}

	@Test
	public void testAutoSizesKeepsDeclaredSizesAsFallback() {

		// The declared value stays behind the keyword so that a browser
		// without automatic sizing still receives a real one.

		SourceDefinition sourceDefinition = _autoSizesSourceDefinition();

		Assert.assertEquals("auto, 100vw", sourceDefinition.getSizes(true));
	}

	@Test
	public void testSizesAreUntouchedWithoutAutoSizes() {

		// Automatic sizing is an opt in. A source that did not ask for it
		// must render the declared value whether or not the image is lazy.

		SourceDefinition sourceDefinition = new SourceDefinition(
			false, "default", null, null, "100vw",
			Collections.<String, String>emptyMap());

		Assert.assertEquals("100vw", sourceDefinition.getSizes(true));
		Assert.assertEquals("100vw", sourceDefinition.getSizes(false));
	}

	private SourceDefinition _autoSizesSourceDefinition() {
		return new SourceDefinition(
			true, "default", null, null, "100vw",
			Collections.<String, String>emptyMap());
	}

}