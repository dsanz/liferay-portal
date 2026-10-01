/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.adaptive.media;

import com.liferay.adaptive.media.image.html.AMImageHTMLTagFactory;
import com.liferay.adaptive.media.image.html.constants.AMImageHTMLConstants;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.FeatureFlag;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.responsive.image.FileEntryImageResource;
import com.liferay.responsive.image.ImageResourceFactory;
import com.liferay.responsive.image.ResponsiveImageMarkupRenderer;
import com.liferay.responsive.image.ResponsiveImageRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Daniel Sanz
 */
@FeatureFlag("LPD-94784")
public class ResponsiveImageAMImageHTMLTagFactoryTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		Mockito.when(
			_imageResourceFactory.fromFileEntry(_fileEntry)
		).thenReturn(
			_fileEntryImageResource
		);

		ReflectionTestUtil.setFieldValue(
			_responsiveImageAMImageHTMLTagFactory, "_amImageHTMLTagFactory",
			_amImageHTMLTagFactory);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageAMImageHTMLTagFactory, "_imageResourceFactory",
			_imageResourceFactory);
		ReflectionTestUtil.setFieldValue(
			_responsiveImageAMImageHTMLTagFactory,
			"_responsiveImageMarkupRenderer", _responsiveImageMarkupRenderer);
	}

	@FeatureFlag(enable = false, value = "LPD-94784")
	@Test
	public void testCreateDelegatesToAdaptiveMediaWhenTheFeatureFlagIsDisabled()
		throws Exception {

		Mockito.when(
			_amImageHTMLTagFactory.create(_ORIGINAL_IMG_TAG, _fileEntry)
		).thenReturn(
			"<picture>adaptive media</picture>"
		);

		Assert.assertEquals(
			"<picture>adaptive media</picture>",
			_responsiveImageAMImageHTMLTagFactory.create(
				_ORIGINAL_IMG_TAG, _fileEntry));

		Mockito.verifyNoInteractions(_responsiveImageMarkupRenderer);
	}

	@Test
	public void testCreateDelegatesToAdaptiveMediaWhenTheFrameworkDeclines()
		throws Exception {

		_whenFrameworkRenders(null);

		Mockito.when(
			_amImageHTMLTagFactory.create(_ORIGINAL_IMG_TAG, _fileEntry)
		).thenReturn(
			"<picture>adaptive media</picture>"
		);

		Assert.assertEquals(
			"<picture>adaptive media</picture>",
			_responsiveImageAMImageHTMLTagFactory.create(
				_ORIGINAL_IMG_TAG, _fileEntry));
	}

	@Test
	public void testCreateMarksThePictureWithTheFileEntryId() throws Exception {
		Mockito.when(
			_fileEntry.getFileEntryId()
		).thenReturn(
			1989L
		);

		_whenFrameworkRenders(
			"<picture><source srcset=\"a 320w\" />" + _ORIGINAL_IMG_TAG +
				"</picture>");

		String markup = _responsiveImageAMImageHTMLTagFactory.create(
			_ORIGINAL_IMG_TAG, _fileEntry);

		Assert.assertTrue(
			markup,
			markup.startsWith(
				"<picture " +
					AMImageHTMLConstants.ATTRIBUTE_NAME_FILE_ENTRY_ID +
						"=\"1989\">"));
		Assert.assertTrue(markup, markup.endsWith("</picture>"));
	}

	@Test
	public void testCreateRendersThroughTheFrameworkWhenTheFeatureFlagIsEnabled()
		throws Exception {

		_whenFrameworkRenders("<img srcset=\"...\" />");

		Assert.assertEquals(
			"<img srcset=\"...\" />",
			_responsiveImageAMImageHTMLTagFactory.create(
				_ORIGINAL_IMG_TAG, _fileEntry));

		Mockito.verifyNoInteractions(_amImageHTMLTagFactory);
	}

	private void _whenFrameworkRenders(String markup) throws Exception {
		Mockito.when(
			_responsiveImageMarkupRenderer.render(
				Mockito.eq(_ORIGINAL_IMG_TAG),
				Mockito.any(ResponsiveImageRequest.class))
		).thenReturn(
			markup
		);
	}

	private static final String _ORIGINAL_IMG_TAG =
		"<img src=\"/documents/1/2/photo.jpg\" />";

	private final AMImageHTMLTagFactory _amImageHTMLTagFactory = Mockito.mock(
		AMImageHTMLTagFactory.class);
	private final FileEntry _fileEntry = Mockito.mock(FileEntry.class);
	private final FileEntryImageResource _fileEntryImageResource = Mockito.mock(
		FileEntryImageResource.class);
	private final ImageResourceFactory _imageResourceFactory = Mockito.mock(
		ImageResourceFactory.class);
	private final ResponsiveImageAMImageHTMLTagFactory
		_responsiveImageAMImageHTMLTagFactory =
			new ResponsiveImageAMImageHTMLTagFactory();
	private final ResponsiveImageMarkupRenderer _responsiveImageMarkupRenderer =
		Mockito.mock(ResponsiveImageMarkupRenderer.class);

}