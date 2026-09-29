/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder.fastly.internal;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Sanz
 */
public class FastlyImageTransformationURLRendererTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testRenderAppendsToExistingQueryString() {
		Assert.assertEquals(
			"/documents/1/2/photo.jpg?version=1.0&width=320",
			_fastlyImageTransformationURLRenderer.render(
				"/documents/1/2/photo.jpg?version=1.0",
				HashMapBuilder.put(
					"width", "320"
				).build()));
	}

	@Test
	public void testRenderDropsParametersFastlyDoesNotDefine() {
		Assert.assertEquals(
			"/photo.jpg?width=320",
			_fastlyImageTransformationURLRenderer.render(
				"/photo.jpg",
				HashMapBuilder.put(
					RandomTestUtil.randomString(), RandomTestUtil.randomString()
				).put(
					"width", "320"
				).build()));
	}

	@Test
	public void testRenderEncodesValues() {
		String url = _fastlyImageTransformationURLRenderer.render(
			"/documents/1/2/photo.jpg",
			HashMapBuilder.put(
				"bg-color", "rgb(1, 2, 3)"
			).build());

		Assert.assertEquals(
			"/documents/1/2/photo.jpg?bg-color=rgb%281%2C+2%2C+3%29", url);
	}

	@Test
	public void testRenderIsDeterministicRegardlessOfInsertionOrder() {
		Assert.assertEquals(
			_fastlyImageTransformationURLRenderer.render(
				"/documents/1/2/photo.jpg",
				LinkedHashMapBuilder.put(
					"format", "webp"
				).put(
					"quality", "80"
				).put(
					"width", "320"
				).build()),
			_fastlyImageTransformationURLRenderer.render(
				"/documents/1/2/photo.jpg",
				LinkedHashMapBuilder.put(
					"width", "320"
				).put(
					"quality", "80"
				).put(
					"format", "webp"
				).build()));
	}

	@Test
	public void testRenderPassesEveryDefinedParameterThrough() {
		Assert.assertEquals(
			"/photo.jpg?orient=6",
			_fastlyImageTransformationURLRenderer.render(
				"/photo.jpg",
				HashMapBuilder.put(
					"orient", "6"
				).build()));
	}

	@Test
	public void testRenderReturnsTheURLWhenNothingIsUnderstood() {
		Assert.assertEquals(
			"/photo.jpg",
			_fastlyImageTransformationURLRenderer.render(
				"/photo.jpg",
				HashMapBuilder.put(
					RandomTestUtil.randomString(), RandomTestUtil.randomString()
				).build()));
	}

	@Test
	public void testRenderReturnsURLUnchangedWhenNothingToApply() {
		Assert.assertEquals(
			"/photo.jpg",
			_fastlyImageTransformationURLRenderer.render(
				"/photo.jpg", Collections.<String, String>emptyMap()));

		Assert.assertEquals(
			"/photo.jpg",
			_fastlyImageTransformationURLRenderer.render("/photo.jpg", null));
	}

	@Test
	public void testRenderSkipsBlankNamesAndValues() {
		Assert.assertEquals(
			"/photo.jpg?width=320",
			_fastlyImageTransformationURLRenderer.render(
				"/photo.jpg",
				HashMapBuilder.put(
					"quality", ""
				).put(
					"width", "320"
				).build()));
	}

	@Test
	public void testRenderSortsParametersCanonically() {
		Assert.assertEquals(
			"/photo.jpg?format=webp&quality=80&width=320",
			_fastlyImageTransformationURLRenderer.render(
				"/photo.jpg",
				HashMapBuilder.put(
					"format", "webp"
				).put(
					"quality", "80"
				).put(
					"width", "320"
				).build()));
	}

	private final FastlyImageTransformationURLRenderer
		_fastlyImageTransformationURLRenderer =
			new FastlyImageTransformationURLRenderer();

}