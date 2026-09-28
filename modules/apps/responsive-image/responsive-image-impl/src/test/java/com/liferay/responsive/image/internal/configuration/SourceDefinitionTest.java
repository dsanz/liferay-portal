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
		SourceDefinition sourceDefinition = _autoSizesSourceDefinition();

		Assert.assertEquals("100vw", sourceDefinition.getSizes(false));
	}

	@Test
	public void testAutoSizesKeepsDeclaredSizesAsFallback() {
		SourceDefinition sourceDefinition = _autoSizesSourceDefinition();

		Assert.assertEquals("auto, 100vw", sourceDefinition.getSizes(true));
	}

	@Test
	public void testSizesAreUntouchedWithoutAutoSizes() {
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