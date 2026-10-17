/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.adaptive.media.image.internal.url;

import com.liferay.adaptive.media.AMURIResolver;
import com.liferay.adaptive.media.image.configuration.AMImageConfigurationEntry;
import com.liferay.adaptive.media.image.internal.configuration.AMImageConfigurationEntryImpl;
import com.liferay.document.library.helper.DLURLHelper;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.test.rule.FeatureFlag;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.net.URI;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Alejandro Tardín
 * @author Daniel Sanz
 */
@FeatureFlag("LPD-94784")
public class AMImageURLFactoryImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		_mockPreviewURL(_ORIGINAL_URL);

		_setUpFileVersion();

		_setUpPortal();

		Mockito.when(
			_amURIResolver.resolveURI(Mockito.any(URI.class))
		).thenAnswer(
			invocation -> URI.create(_URI_PREFIX + invocation.getArguments()[0])
		);

		ReflectionTestUtil.setFieldValue(
			_amImageURLFactoryImpl, "_amURIResolver", _amURIResolver);
		ReflectionTestUtil.setFieldValue(
			_amImageURLFactoryImpl, "_dlURLHelper", _dlURLHelper);
		ReflectionTestUtil.setFieldValue(
			_amImageURLFactoryImpl, "_portal", _portal);
	}

	@Test
	public void testCreateFileEntryURLAddsTheAdaptationURLParametersWhenAdaptationIsDelegated() {
		Assert.assertEquals(
			_ORIGINAL_URL + "?" + _QUERY_STRINGS[0],
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(
						_QUERY_STRINGS[0], "true"))));
	}

	@Test
	public void testCreateFileEntryURLIgnoresTheAdaptationURLParametersWhenAdaptationIsNotDelegated() {
		Assert.assertEquals(
			_getAdaptedImageURL(),
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(
						_QUERY_STRINGS[1], "false"))));
	}

	@Test
	public void testCreateFileEntryURLIgnoresTheCompanyCDNHostForAnAbsoluteURL()
		throws Exception {

		_mockCDNHostHttps(_CDN_HOST);

		String[] urls = {
			StringBundler.concat(
				"//", RandomTestUtil.randomString(), "/",
				RandomTestUtil.randomString()),
			StringBundler.concat(
				"http://", RandomTestUtil.randomString(), "/",
				RandomTestUtil.randomString()),
			StringBundler.concat(
				"https://", RandomTestUtil.randomString(), "/",
				RandomTestUtil.randomString())
		};

		for (String url : urls) {
			_mockPreviewURL(url);

			Assert.assertEquals(
				url + "?" + _QUERY_STRINGS[0],
				String.valueOf(
					_amImageURLFactoryImpl.createFileEntryURL(
						_fileVersion,
						_createAMImageConfigurationEntry(
							_QUERY_STRINGS[0], "true"))));
		}
	}

	@Test
	public void testCreateFileEntryURLIgnoresTheCompanyCDNHostWhenAdaptationIsNotDelegated() {
		_mockCDNHostHttps(_CDN_HOST);

		Assert.assertEquals(
			_getAdaptedImageURL(),
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(null, "false"))));
	}

	@FeatureFlag(enable = false, value = "LPD-94784")
	@Test
	public void testCreateFileEntryURLIgnoresThePropertiesWhenTheFeatureFlagIsDisabled() {
		Assert.assertEquals(
			_getAdaptedImageURL(),
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(
						_QUERY_STRINGS[0], "true"))));
	}

	@Test
	public void testCreateFileEntryURLIgnoresTheVersionWhenAdaptationIsDelegated()
		throws Exception {

		_amImageURLFactoryImpl.createFileEntryURL(
			_fileVersion,
			_createAMImageConfigurationEntry(_QUERY_STRINGS[0], "true"));

		_verifyPreviewURL(false);
	}

	@Test
	public void testCreateFileEntryURLIncludesTheCompanyCDNHostHttpWhenAdaptationIsDelegated() {
		Mockito.when(
			_portal.getCDNHostHttp(_COMPANY_ID)
		).thenReturn(
			_CDN_HOST
		);

		Assert.assertEquals(
			StringBundler.concat(
				_CDN_HOST, _ORIGINAL_URL, "?", _QUERY_STRINGS[1]),
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(
						_QUERY_STRINGS[1], "true"))));
	}

	@Test
	public void testCreateFileEntryURLPointsToAdaptedImageWhenAdaptationIsNotDelegated() {
		Assert.assertEquals(
			_getAdaptedImageURL(),
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(null, "false"))));
	}

	@Test
	public void testCreateFileEntryURLStripsTheTrailingSlashFromTheCompanyCDNHost() {
		_mockCDNHostHttps(_CDN_HOST + "/");

		Assert.assertEquals(
			StringBundler.concat(
				_CDN_HOST, _ORIGINAL_URL, "?", _QUERY_STRINGS[0]),
			String.valueOf(
				_amImageURLFactoryImpl.createFileEntryURL(
					_fileVersion,
					_createAMImageConfigurationEntry(
						_QUERY_STRINGS[0], "true"))));
	}

	@Test
	public void testCreateFileVersionURLIncludesTheVersionWhenAdaptationIsDelegated()
		throws Exception {

		Assert.assertEquals(
			_ORIGINAL_URL + "?" + _QUERY_STRINGS[0],
			String.valueOf(
				_amImageURLFactoryImpl.createFileVersionURL(
					_fileVersion,
					_createAMImageConfigurationEntry(
						_QUERY_STRINGS[0], "true"))));

		_verifyPreviewURL(true);
	}

	@Test
	public void testCreateFileVersionURLPointsToAdaptedImageWhenAdaptationIsNotDelegated() {
		Assert.assertEquals(
			StringBundler.concat(
				_URI_PREFIX, "image/", _FILE_ENTRY_ID, "/", _FILE_VERSION_ID,
				"/", _UUID, "/", _FILE_NAME),
			String.valueOf(
				_amImageURLFactoryImpl.createFileVersionURL(
					_fileVersion,
					_createAMImageConfigurationEntry(null, null))));
	}

	private AMImageConfigurationEntry _createAMImageConfigurationEntry(
		String adaptationURLParameters, String delegateImageAdaptation) {

		Map<String, String> properties = new HashMap<>();

		if (adaptationURLParameters != null) {
			properties = HashMapBuilder.putAll(
				properties
			).put(
				"adaptation-url-parameters", adaptationURLParameters
			).build();
		}

		if (delegateImageAdaptation != null) {
			properties = HashMapBuilder.putAll(
				properties
			).put(
				"delegate-image-adaptation", delegateImageAdaptation
			).build();
		}

		return new AMImageConfigurationEntryImpl(
			RandomTestUtil.randomString(), _UUID, properties);
	}

	private String _getAdaptedImageURL() {
		return StringBundler.concat(
			_URI_PREFIX, "image/", _FILE_ENTRY_ID, "/", _UUID, "/", _FILE_NAME,
			"?t=", _modifiedDate.getTime());
	}

	private void _mockCDNHostHttps(String cdnHost) {
		Mockito.when(
			_portal.getCDNHostHttps(_COMPANY_ID)
		).thenReturn(
			cdnHost
		);
	}

	private void _mockPreviewURL(String url) throws Exception {
		Mockito.when(
			_dlURLHelper.getPreviewURL(
				Mockito.eq(_fileEntry), Mockito.eq(_fileVersion),
				Mockito.isNull(), Mockito.anyString(), Mockito.anyBoolean(),
				Mockito.anyBoolean())
		).thenReturn(
			url
		);
	}

	private void _setUpFileVersion() throws Exception {
		Mockito.when(
			_fileVersion.getCompanyId()
		).thenReturn(
			_COMPANY_ID
		);

		Mockito.when(
			_fileVersion.getFileEntry()
		).thenReturn(
			_fileEntry
		);

		Mockito.when(
			_fileVersion.getFileEntryId()
		).thenReturn(
			_FILE_ENTRY_ID
		);

		Mockito.when(
			_fileVersion.getFileName()
		).thenReturn(
			_FILE_NAME
		);

		Mockito.when(
			_fileVersion.getFileVersionId()
		).thenReturn(
			_FILE_VERSION_ID
		);

		Mockito.when(
			_fileVersion.getModifiedDate()
		).thenReturn(
			_modifiedDate
		);
	}

	private void _setUpPortal() {
		Mockito.when(
			_portal.stripURLAnchor(Mockito.anyString(), Mockito.anyString())
		).thenAnswer(
			invocation -> new String[] {
				(String)invocation.getArguments()[0], ""
			}
		);

		PortalUtil portalUtil = new PortalUtil();

		portalUtil.setPortal(_portal);
	}

	private void _verifyPreviewURL(boolean appendVersion) throws Exception {
		Mockito.verify(
			_dlURLHelper
		).getPreviewURL(
			Mockito.eq(_fileEntry), Mockito.eq(_fileVersion), Mockito.isNull(),
			Mockito.anyString(), Mockito.eq(appendVersion), Mockito.anyBoolean()
		);
	}

	private static final String _CDN_HOST =
		"https://" + RandomTestUtil.randomString();

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private static final long _FILE_ENTRY_ID = RandomTestUtil.randomLong();

	private static final String _FILE_NAME = RandomTestUtil.randomString();

	private static final long _FILE_VERSION_ID = RandomTestUtil.randomLong();

	private static final String _ORIGINAL_URL =
		"/" + RandomTestUtil.randomString();

	private static final String[] _QUERY_STRINGS = {
		StringBundler.concat(
			RandomTestUtil.randomString(), "=", RandomTestUtil.randomString()),
		StringBundler.concat(
			RandomTestUtil.randomString(), "=", RandomTestUtil.randomString(),
			"&", RandomTestUtil.randomString(), "=",
			RandomTestUtil.randomString())
	};

	private static final String _URI_PREFIX =
		RandomTestUtil.randomString() + "/";

	private static final String _UUID = RandomTestUtil.randomString();

	private final AMImageURLFactoryImpl _amImageURLFactoryImpl =
		new AMImageURLFactoryImpl();
	private final AMURIResolver _amURIResolver = Mockito.mock(
		AMURIResolver.class);
	private final DLURLHelper _dlURLHelper = Mockito.mock(DLURLHelper.class);
	private final FileEntry _fileEntry = Mockito.mock(FileEntry.class);
	private final FileVersion _fileVersion = Mockito.mock(FileVersion.class);
	private final Date _modifiedDate = new Date();
	private final Portal _portal = Mockito.mock(Portal.class);

}