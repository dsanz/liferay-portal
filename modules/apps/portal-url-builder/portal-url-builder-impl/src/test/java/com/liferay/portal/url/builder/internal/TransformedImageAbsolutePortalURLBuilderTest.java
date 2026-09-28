/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.frontend.hashed.files.CachingStrategy;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.Mockito;

/**
 * @author Daniel Sanz
 */
@RunWith(Parameterized.class)
public class TransformedImageAbsolutePortalURLBuilderTest
	extends BaseAbsolutePortalURLBuilderTestCase {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Parameterized.Parameters(name = "{0}: cdnHost={1}, context={2}, proxy={3}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			new Object[][] {
				{0, false, false, false}, {1, false, false, true},
				{2, false, true, false}, {3, false, true, true},
				{4, true, false, false}
			});
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();

		_absolutePortalURLBuilder = new AbsolutePortalURLBuilderImpl(
			mockCacheHelper(),
			mockHashedFilesRegistry(CachingStrategy.DO_NOT_USE_HASHES),
			mockPortal(context, proxy, cdnHost), mockHttpServletRequest());

		_transformedImageAbsolutePortalURLBuilder =
			_absolutePortalURLBuilder.forTransformedImage(
				"/documents/d/guest/image.png");
	}

	@After
	public void tearDown() {
		super.tearDown();
	}

	@Test
	public void test() {
		Assert.assertEquals(
			_RESULTS[index], _transformedImageAbsolutePortalURLBuilder.build());
	}

	@Test
	public void testAddingATransformationTwiceReplacesTheValue() {
		_transformedImageAbsolutePortalURLBuilder.addTransformation(
			"width", "320"
		).addTransformation(
			"width", "640"
		).setRenderer(
			_imageTransformationURLRenderer
		);

		Assert.assertEquals(
			_RESULTS[index] + "?width=640",
			_transformedImageAbsolutePortalURLBuilder.build());
	}

	@Test
	public void testIgnoreCDN() {
		_transformedImageAbsolutePortalURLBuilder.ignoreCDNHost();

		Assert.assertEquals(
			_RESULTS_IGNORE_CDN[index],
			_transformedImageAbsolutePortalURLBuilder.build());
	}

	@Test
	public void testNullTransformationsAreIgnored() {
		_transformedImageAbsolutePortalURLBuilder.addTransformation(
			null, "320"
		).addTransformation(
			"quality", null
		).setRenderer(
			_imageTransformationURLRenderer
		);

		Assert.assertEquals(
			_RESULTS[index], _transformedImageAbsolutePortalURLBuilder.build());

		Mockito.verifyNoInteractions(_imageTransformationURLRenderer);
	}

	@Test
	public void testRendererIsNotConsultedWithoutTransformations() {
		_transformedImageAbsolutePortalURLBuilder.setRenderer(
			_imageTransformationURLRenderer);

		Assert.assertEquals(
			_RESULTS[index], _transformedImageAbsolutePortalURLBuilder.build());

		Mockito.verifyNoInteractions(_imageTransformationURLRenderer);
	}

	@Test
	public void testRendererReceivesTheURLAndTheTransformations() {
		_transformedImageAbsolutePortalURLBuilder.addTransformation(
			"width", "320"
		).addTransformation(
			"quality", "80"
		).setRenderer(
			_imageTransformationURLRenderer
		);

		Assert.assertEquals(
			_RESULTS[index] + "?quality=80&width=320",
			_transformedImageAbsolutePortalURLBuilder.build());
	}

	@Test
	public void testTransformationsAreDroppedWithoutARenderer() {
		_transformedImageAbsolutePortalURLBuilder.addTransformation(
			"width", "320"
		).setRenderer(
			null
		);

		Assert.assertEquals(
			_RESULTS[index], _transformedImageAbsolutePortalURLBuilder.build());
	}

	@Test
	public void testTransformationsAreSpelledInACanonicalOrder() {
		_transformedImageAbsolutePortalURLBuilder.addTransformation(
			"width", "320"
		).addTransformation(
			"quality", "80"
		).setRenderer(
			_imageTransformationURLRenderer
		);

		TransformedImageAbsolutePortalURLBuilder
			transformedImageAbsolutePortalURLBuilder =
				_absolutePortalURLBuilder.forTransformedImage(
					"/documents/d/guest/image.png"
				).addTransformation(
					"quality", "80"
				).addTransformation(
					"width", "320"
				).setRenderer(
					_imageTransformationURLRenderer
				);

		Assert.assertEquals(
			_transformedImageAbsolutePortalURLBuilder.build(),
			transformedImageAbsolutePortalURLBuilder.build());
	}

	@Parameterized.Parameter(1)
	public boolean cdnHost;

	@Parameterized.Parameter(2)
	public boolean context;

	@Parameterized.Parameter
	public int index;

	@Parameterized.Parameter(3)
	public boolean proxy;

	private static final String[] _RESULTS = {
		"/documents/d/guest/image.png", "/proxy/documents/d/guest/image.png",
		"/documents/d/guest/image.png", "/proxy/documents/d/guest/image.png",
		"http://cdn-host/documents/d/guest/image.png"
	};

	private static final String[] _RESULTS_IGNORE_CDN = {
		"/documents/d/guest/image.png", "/proxy/documents/d/guest/image.png",
		"/documents/d/guest/image.png", "/proxy/documents/d/guest/image.png",
		"/documents/d/guest/image.png"
	};

	private AbsolutePortalURLBuilder _absolutePortalURLBuilder;

	private final ImageTransformationURLRenderer
		_imageTransformationURLRenderer = Mockito.mock(
			ImageTransformationURLRenderer.class,
			invocation -> {
				Map<String, String> transformations = invocation.getArgument(1);

				StringBuilder sb = new StringBuilder(
					invocation.getArgument(0, String.class));

				sb.append('?');

				transformations.forEach(
					(name, value) -> sb.append(
						StringBundler.concat(name, "=", value, "&")));

				sb.setLength(sb.length() - 1);

				return sb.toString();
			});

	private TransformedImageAbsolutePortalURLBuilder
		_transformedImageAbsolutePortalURLBuilder;

}