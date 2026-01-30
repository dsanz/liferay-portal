/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.vulcan.util;

import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Random;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Szimko
 */
public class ObjectMapperUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testReadValueWithLargeBase64String() {
		Random random = new Random();

		byte[] bytes = new byte[21_000_000];

		random.nextBytes(bytes);

		String string = Base64.encode(bytes);

		TestClass testClass = ObjectMapperUtil.readValue(
			TestClass.class,
			HashMapBuilder.<String, Object>put(
				"string", string
			).build());

		Assert.assertEquals(string, testClass.string);
	}

	public static class TestClass {

		public String string;

	}

}