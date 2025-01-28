/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.view;

import com.liferay.client.extension.type.manager.CETManager;
import com.liferay.frontend.data.set.internal.serializer.BaseCustomFDSSerializer;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.liferay.poshi.core.util.ListUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Daniel Sanz
 */
public class CustomFDSViewSerializerImplTest {

		@ClassRule
		@Rule
		public static final LiferayUnitTestRule liferayUnitTestRule =
			LiferayUnitTestRule.INSTANCE;

		// TODO abstract out locale and theme display init into a super base class
		@Before
		public void setUp() throws Exception {
			_customFDSViewSerializerImpl = Mockito.mock(
				CustomFDSViewSerializerImpl.class);
			ReflectionTestUtil.setFieldValue(
				_customFDSViewSerializerImpl, "_jsonFactory", _jsonFactory);
			ReflectionTestUtil.setFieldValue(
				_customFDSViewSerializerImpl, "_cetManager", _cetManager);
			ReflectionTestUtil.setFieldValue(
				_customFDSViewSerializerImpl, "_language", _language);

			ReflectionTestUtil.setFieldValue(
				_customFDSViewSerializerImpl, "_portal", _portal);

			ThemeDisplay themeDisplay = Mockito.mock(ThemeDisplay.class);

			Mockito.when(
				themeDisplay.getCompanyId()
			).thenReturn(
				0L
			);
			Mockito.when(
				_httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY)
			).thenReturn(
				themeDisplay
			);
			LanguageUtil languageUtil = new LanguageUtil();

			languageUtil.setLanguage(_language);

			Mockito.when(
				_portal.getLocale(_httpServletRequest)
			).thenReturn(
				LocaleUtil.US
			);

			Mockito.when(
				_language.get(LocaleUtil.US, null)
			).thenReturn(
				StringPool.BLANK
			);

			Mockito.when(
				_language.get(Mockito.eq(LocaleUtil.US), Mockito.anyString())
			).thenAnswer(
				invocation -> invocation.getArgument(1, String.class)
			);

			Mockito.when(
				_language.get(
					Mockito.eq(ResourceBundleUtil.EMPTY_RESOURCE_BUNDLE),
					Mockito.anyString())
			).thenAnswer(
				invocation -> invocation.getArgument(1, String.class)
			);

		}

		@Test
		public void testFDSCardsViewSerialization() throws Exception {
			_mockFDSCardsSectionObjectEntries(
				"fdsName", HashMapBuilder.put("name", "title").put("thumbnail", "image").build());

			System.out.println(_customFDSViewSerializerImpl.serialize(
				"fdsName", _httpServletRequest
			).toString());

			JSONAssert.assertEquals(
				JSONUtil.putAll(
					JSONUtil.put(
						"contentRenderer", "cards"
					).put(
						"default", false
					).put(
						"name", "cards"
					).put(
						"schema",
						JSONUtil.put(
							"image", "thumbnail"
						).put(
							"title", "name"
						)
					).put(
						"thumbnail", "cards2"
					)
				).toString(),
				_customFDSViewSerializerImpl.serialize(
					"fdsName", _httpServletRequest
				).toString(),
				JSONCompareMode.LENIENT);
		}

		@Test
		public void testFDSListViewSerialization() throws Exception {
			_mockFDSListSectionObjectEntries(
				"fdsName", HashMapBuilder.put("name", "title").put("thumbnail", "image").build());

			System.out.println(_customFDSViewSerializerImpl.serialize(
				"fdsName", _httpServletRequest
			).toString());
			JSONAssert.assertEquals(
				JSONUtil.putAll(
					JSONUtil.put(
						"contentRenderer", "list"
					).put(
						"default", false
					).put(
						"name", "list"
					).put(
						"schema",
						JSONUtil.put(
							"image", "thumbnail"
						).put(
							"title", "name"
						)
					).put(
						"thumbnail", "list"
					)
				).toString(),
				_customFDSViewSerializerImpl.serialize(
					"fdsName", _httpServletRequest
				).toString(),
				JSONCompareMode.LENIENT);
		}

	@Test
	public void testFDSTableViewSerialization() throws Exception {
		List<Map<String, Object>> tableSectionObjectEntriesProperties = new ArrayList();

		tableSectionObjectEntriesProperties.add(_createTableSectionObjectEntryProperties());

		_mockFDSTableSectionObjectEntries(
			"fdsName", tableSectionObjectEntriesProperties);

		System.out.println(_customFDSViewSerializerImpl.serialize(
			"fdsName", _httpServletRequest
		).toString());
		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "list"
				).put(
					"default", false
				).put(
					"name", "list"
				).put(
					"schema",
					JSONUtil.put(
						"image", "thumbnail"
					).put(
						"title", "name"
					)
				).put(
					"thumbnail", "list"
				)
			).toString(),
			_customFDSViewSerializerImpl.serialize(
				"fdsName", _httpServletRequest
			).toString(),
			JSONCompareMode.LENIENT);
	}


	@Test
		public void testFDSViewSerializationNoView() throws Exception {
			_mockFDSEmptyViewObjectEntry("fdsName");

			JSONAssert.assertEquals(
				"[]",
				_customFDSViewSerializerImpl.serialize(
					"fdsName", _httpServletRequest
				).toString(),
				JSONCompareMode.STRICT);
		}
/*
		@Test
		public void testFDSViewSerializationSeparateViews() throws Exception {
			_mockFDSDateRangeFilterObjectEntry(
				"fdsName1", "createDate", "By Creation Date",
				FDSEntityFieldTypes.DATE, "2000-12-31T00:00:00.000Z", null);
			_mockFDSDateRangeFilterObjectEntry(
				"fdsName2", "modifiedDate", "By Modification Date",
				FDSEntityFieldTypes.DATE, null, "2025-10-03T00:00:00.000Z");
			JSONAssert.assertEquals(
				JSONUtil.putAll(
					JSONUtil.put(
						"active", true
					).put(
						"entityFieldType", "date"
					).put(
						"id", "createDate"
					).put(
						"label", "By Creation Date"
					).put(
						"preloadedData",
						JSONUtil.put(
							"from",
							JSONUtil.put(
								"day", 31
							).put(
								"month", 12
							).put(
								"year", 2000
							))
					).put(
						"type", "dateRange"
					)
				).toString(),
				_customFDSViewSerializerImpl.serialize(
					"fdsName1", _httpServletRequest
				).toString(),
				JSONCompareMode.LENIENT);
			JSONAssert.assertEquals(
				JSONUtil.putAll(
					JSONUtil.put(
						"active", true
					).put(
						"entityFieldType", "date"
					).put(
						"id", "modifiedDate"
					).put(
						"label", "By Modification Date"
					).put(
						"preloadedData",
						JSONUtil.put(
							"to",
							JSONUtil.put(
								"day", 3
							).put(
								"month", 10
							).put(
								"year", 2025
							))
					).put(
						"type", "dateRange"
					)
				).toString(),
				_customFDSViewSerializerImpl.serialize(
					"fdsName2", _httpServletRequest
				).toString(),
				JSONCompareMode.LENIENT);
		}

		@Test
		public void testFDSTableViewSerialization() throws Exception {
			_mockFDSSelectionFilterObjectEntry(
				"fdsName", "channelId", true, "channelId", "name", "By Channel",
				true, "[{\"label\":\"site 1\",\"value\":\"20192\"}]",
				"/analytics-settings-rest/v1.0", "/v1.0/channels", "Channel",
				"/o/analytics-settings-rest/v1.0/channels", "API_REST_APPLICATION");
			JSONAssert.assertEquals(
				JSONUtil.putAll(
					JSONUtil.put(
						"apiURL", "/o/analytics-settings-rest/v1.0/channels"
					).put(
						"autocompleteEnabled", true
					).put(
						"entityFieldType", "string"
					).put(
						"id", "channelId"
					).put(
						"itemKey", "channelId"
					).put(
						"itemLabel", "name"
					).put(
						"label", "By Channel"
					).put(
						"multiple", true
					).put(
						"preloadedData",
						JSONUtil.put(
							"exclude", false
						).put(
							"selectedItems",
							JSONUtil.putAll(
								JSONUtil.put(
									"label", "site 1"
								).put(
									"value", "20192"
								))
						)
					).put(
						"type", "selection"
					)
				).toString(),
				_customFDSViewSerializerImpl.serialize(
					"fdsName", _httpServletRequest
				).toString(),
				JSONCompareMode.LENIENT);
		}

		private void _mockFDSTableSectionObjectEntry(
			String fdsName, String fieldName, String name,
			String rendererName) {

			Mockito.when(
				_customFDSViewSerializerImpl.serialize(
					fdsName, _httpServletRequest)
			).thenCallRealMethod();
			BaseCustomFDSSerializer baseCustomFDSSerializer =
				(BaseCustomFDSSerializer) _customFDSViewSerializerImpl;

			Mockito.when(
				baseCustomFDSSerializer.getLabelValue(
					Mockito.eq("label"), Mockito.eq("fieldName"), Mockito.anyMap())
			).thenCallRealMethod();
			Set<ObjectEntry> objectEntries = new HashSet<>();
			ObjectEntry objectEntry = new ObjectEntry();

			objectEntry.setProperties(
				HashMapBuilder.put(
					"fieldName", (Object)fieldName
				).put(
					"name", (Object)name
				).put(
					"rendererName", (Object)rendererName
				).build());
			objectEntries.add(objectEntry);
			Mockito.when(
				baseCustomFDSSerializer.getFilterObjectEntries(
					fdsName, _httpServletRequest)
			).thenReturn(
				objectEntries
			);
			Mockito.when(
				_cetManager.getCET(
					Mockito.anyLong(), Mockito.eq(clientExtensionEntryERC))
			).thenAnswer(
				invocation -> new FDSFilterCET() {

					@Override
					public String getBaseURL() {
						return "";
					}

					@Override
					public long getCompanyId() {
						return invocation.getArgument(0, long.class);
					}

					@Override
					public Date getCreateDate() {
						return null;
					}

					@Override
					public String getDescription() {
						return "";
					}

					@Override
					public String getEditJSP() {
						return "";
					}

					@Override
					public String getExternalReferenceCode() {
						return clientExtensionEntryERC;
					}

					@Override
					public Date getModifiedDate() {
						return null;
					}

					@Override
					public String getName() {
						return "";
					}

					@Override
					public String getName(Locale locale) {
						return "";
					}

					@Override
					public Properties getProperties() {
						return null;
					}

					@Override
					public String getSourceCodeURL() {
						return "";
					}

					@Override
					public int getStatus() {
						return 0;
					}

					@Override
					public String getType() {
						return "";
					}

					@Override
					public String getTypeSettings() {
						return "";
					}

					@Override
					public String getURL() {
						return "/o/" + clientExtensionEntryERC + "/index.js";
					}

					@Override
					public boolean hasProperties() {
						return false;
					}

					@Override
					public boolean isReadOnly() {
						return false;
					}

				}
			);
		}
*/
	private void _mockFDSCardsSectionObjectEntries(
		String fdsName, Map<String, String> sectionMappings) {

		Mockito.when(
			_customFDSViewSerializerImpl.serialize(
				fdsName, _httpServletRequest)
		).thenCallRealMethod();
		BaseCustomFDSSerializer baseCustomFDSSerializer =
			(BaseCustomFDSSerializer) _customFDSViewSerializerImpl;

		Mockito.when(
			baseCustomFDSSerializer.getLabelValue(
				Mockito.eq("label"), Mockito.eq("fieldName"), Mockito.anyMap())
		).thenCallRealMethod();
		Set<ObjectEntry> objectEntries = new HashSet<>();

		for (String key : sectionMappings.keySet()) {
			ObjectEntry objectEntry = new ObjectEntry();

			objectEntry.setProperties(
				HashMapBuilder.put(
					"fieldName", (Object)key
				).put(
					"name", (Object)sectionMappings.get(key)
				).build());

			objectEntries.add(objectEntry);
		}

		Mockito.when(
			baseCustomFDSSerializer.getDataSetCardSectionObjectEntries(
				fdsName, _httpServletRequest)
		).thenReturn(
			objectEntries
		);
	}

	private void _mockFDSListSectionObjectEntries(
		String fdsName, Map<String, String> sectionMappings) {

		Mockito.when(
			_customFDSViewSerializerImpl.serialize(
				fdsName, _httpServletRequest)
		).thenCallRealMethod();
		BaseCustomFDSSerializer baseCustomFDSSerializer =
			(BaseCustomFDSSerializer) _customFDSViewSerializerImpl;

		Mockito.when(
			baseCustomFDSSerializer.getLabelValue(
				Mockito.eq("label"), Mockito.eq("fieldName"), Mockito.anyMap())
		).thenCallRealMethod();
		Set<ObjectEntry> objectEntries = new HashSet<>();

		for (String key : sectionMappings.keySet()) {
			ObjectEntry objectEntry = new ObjectEntry();

			objectEntry.setProperties(
				HashMapBuilder.put(
					"fieldName", (Object)key
				).put(
					"name", (Object)sectionMappings.get(key)
				).build());

			objectEntries.add(objectEntry);
		}

		Mockito.when(
			baseCustomFDSSerializer.getDataSetListSectionObjectEntries(
				fdsName, _httpServletRequest)
		).thenReturn(
			objectEntries
		);
	}

		private void _mockFDSEmptyViewObjectEntry(String fdsName) {
			Mockito.when(
				_customFDSViewSerializerImpl.serialize(
					fdsName, _httpServletRequest)
			).thenCallRealMethod();

			BaseCustomFDSSerializer baseCustomFDSSerializer =
				(BaseCustomFDSSerializer) _customFDSViewSerializerImpl;

			Mockito.when(
				baseCustomFDSSerializer.getFilterObjectEntries(
					fdsName, _httpServletRequest)
			).thenReturn(
				Collections.emptySet()
			);
		}

		private Map<String, Object> _createTableSectionObjectEntryProperties(
			String fieldName, String label, String renderer,
			String rendererType, boolean sortable, String type
		) {
			return HashMapBuilder.put(
				"fieldName", (Object)fieldName
			).put(
				"label", (Object)label
			).put(
				"renderer", (Object)renderer
			).put(
				"rendererType", (Object)rendererType
			).put(
				"sortable", (Object)sortable
			).put(
				"type", (Object)type
			).build());
		}

		private void _mockFDSTableSectionObjectEntries(
			String fdsName, List<Map<String, Object>> tableSectionObjectEntriesProperties) {

			Mockito.when(
				_customFDSViewSerializerImpl.serialize(
					fdsName, _httpServletRequest)
			).thenCallRealMethod();
			BaseCustomFDSSerializer baseCustomFDSSerializer =
				(BaseCustomFDSSerializer) _customFDSViewSerializerImpl;

			Mockito.when(
				baseCustomFDSSerializer.getLabelValue(
					Mockito.eq("label"), Mockito.eq("fieldName"), Mockito.anyMap())
			).thenCallRealMethod();
			Set<ObjectEntry> objectEntries = new HashSet<>();

			for (Map<String, Object> tableSectionObjectEntryProperties : tableSectionObjectEntriesProperties) {
				ObjectEntry objectEntry = new ObjectEntry();

				objectEntry.setProperties(tableSectionObjectEntryProperties);

				objectEntries.add(objectEntry);
			}
			Mockito.when(
				baseCustomFDSSerializer.getDataSetTableSectionObjectEntries(
					fdsName, _httpServletRequest)
			).thenReturn(
				objectEntries
			);
		}

		private static CustomFDSViewSerializerImpl _customFDSViewSerializerImpl;
		private static final HttpServletRequest _httpServletRequest = Mockito.mock(
			HttpServletRequest.class);
		private static final JSONFactory _jsonFactory = new JSONFactoryImpl();
	private static final CETManager _cetManager = Mockito.mock(
		CETManager.class);
		private static Portal _portal = Mockito.mock(Portal.class);
	private static final Language _language = Mockito.mock(Language.class);

}
