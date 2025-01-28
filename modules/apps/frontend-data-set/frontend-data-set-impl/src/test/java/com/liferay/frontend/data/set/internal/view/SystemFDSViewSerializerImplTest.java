/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.view;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.frontend.data.set.constants.FDSConstants;
import com.liferay.frontend.data.set.internal.BaseSystemFDSSerializerTestCase;
import com.liferay.frontend.data.set.internal.view.cards.CardsFDSViewContextContributor;
import com.liferay.frontend.data.set.internal.view.list.ListFDSViewContextContributor;
import com.liferay.frontend.data.set.internal.view.table.FDSTableSchemaBuilderImpl;
import com.liferay.frontend.data.set.internal.view.table.TableFDSViewContextContributor;
import com.liferay.frontend.data.set.view.FDSView;
import com.liferay.frontend.data.set.view.FDSViewContextContributor;
import com.liferay.frontend.data.set.view.cards.BaseCardsFDSView;
import com.liferay.frontend.data.set.view.list.BaseListFDSView;
import com.liferay.frontend.data.set.view.table.BaseTableFDSView;
import com.liferay.frontend.data.set.view.table.FDSTableSchema;
import com.liferay.frontend.data.set.view.table.FDSTableSchemaBuilder;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerCustomizerFactory;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.osgi.framework.ServiceRegistration;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Daniel Sanz
 */
public class SystemFDSViewSerializerImplTest
	extends BaseSystemFDSSerializerTestCase {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		_viewServiceTrackerMap = ServiceTrackerMapFactory.openMultiValueMap(
			bundleContext, FDSView.class, "frontend.data.set.name",
			ServiceTrackerCustomizerFactory.<FDSView>serviceWrapper(
				bundleContext));

		_viewContextContributorServiceTrackerMap =
			ServiceTrackerMapFactory.openMultiValueMap(
				bundleContext, FDSViewContextContributor.class,
				"frontend.data.set.view.name",
				ServiceTrackerCustomizerFactory.
					<FDSViewContextContributor>serviceWrapper(bundleContext));

		ReflectionTestUtil.setFieldValue(
			_fdsViewRegistryImpl, "_serviceTrackerMap", _viewServiceTrackerMap);

		ReflectionTestUtil.setFieldValue(
			_fdsViewContextContributorRegistryImpl, "_serviceTrackerMap",
			_viewContextContributorServiceTrackerMap);

		ReflectionTestUtil.setFieldValue(
			_systemFDSViewSerializerImpl, "_fdsViewRegistry",
			_fdsViewRegistryImpl);

		ReflectionTestUtil.setFieldValue(
			_systemFDSViewSerializerImpl, "_fdsViewContextContributorRegistry",
			_fdsViewContextContributorRegistryImpl);

		ReflectionTestUtil.setFieldValue(
			_systemFDSViewSerializerImpl, "_jsonFactory", _jsonFactory);

		ReflectionTestUtil.setFieldValue(
			_systemFDSViewSerializerImpl, "_language", language);

		ReflectionTestUtil.setFieldValue(
			_systemFDSViewSerializerImpl, "_portal", portal);

		ReflectionTestUtil.setFieldValue(
			_tableFDSViewContextContributor, "_jsonFactory", _jsonFactory);

		ReflectionTestUtil.setFieldValue(
			_tableFDSViewContextContributor, "_language", language);

		_cardsFDSViewContextContributorServiceRegistration =
			bundleContext.registerService(
				FDSViewContextContributor.class,
				_cardsFDSViewContextContributor,
				MapUtil.singletonDictionary(
					"frontend.data.set.view.name", FDSConstants.CARDS));

		_listFDSViewContextContributorServiceRegistration =
			bundleContext.registerService(
				FDSViewContextContributor.class, _listFDSViewContextContributor,
				MapUtil.singletonDictionary(
					"frontend.data.set.view.name", FDSConstants.LIST));

		_tableFDSViewContextContributorServiceRegistration =
			bundleContext.registerService(
				FDSViewContextContributor.class,
				_tableFDSViewContextContributor,
				MapUtil.singletonDictionary(
					"frontend.data.set.view.name", FDSConstants.TABLE));
	}

	@After
	public void tearDown() {
		super.tearDown();

		_cardsFDSViewContextContributorServiceRegistration.unregister();
		_listFDSViewContextContributorServiceRegistration.unregister();
		_tableFDSViewContextContributorServiceRegistration.unregister();

		_viewServiceTrackerMap.close();
	}

	@Test
	public void testFDSCardsViewSerialization() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		ServiceRegistration<FDSView> cardsViewServiceRegistration =
			_registerView(
				"fdsName",
				_createCardsView(
					"longDescription", "detailURL", "thumbnail", "sticker",
					"icon", "title"));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "cards"
				).put(
					"default", false
				).put(
					"label", "cards"
				).put(
					"name", "cards"
				).put(
					"schema",
					JSONUtil.put(
						"description", "longDescription"
					).put(
						"href", "detailURL"
					).put(
						"image", "thumbnail"
					).put(
						"sticker", "sticker"
					).put(
						"symbol", "icon"
					).put(
						"title", "title"
					)
				).put(
					"thumbnail", "cards2"
				)
			).toString(),
			_systemFDSViewSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).toString(),
			JSONCompareMode.LENIENT);

		cardsViewServiceRegistration.unregister();

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSListViewSerialization() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		ServiceRegistration<FDSView> listViewServiceRegistration =
			_registerView(
				"fdsName",
				_createListView(
					"longDescription", "thumbnail", "sticker", "icon",
					"title"));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "list"
				).put(
					"default", false
				).put(
					"label", "list"
				).put(
					"name", "list"
				).put(
					"schema",
					JSONUtil.put(
						"description", "longDescription"
					).put(
						"image", "thumbnail"
					).put(
						"sticker", "sticker"
					).put(
						"symbol", "icon"
					).put(
						"title", "title"
					)
				).put(
					"thumbnail", "list"
				)
			).toString(),
			_systemFDSViewSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).toString(),
			JSONCompareMode.LENIENT);

		listViewServiceRegistration.unregister();

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSTableViewSerialization() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		ServiceRegistration<FDSView> selectionFilterServiceRegistration =
			_registerView(
				"fdsName",
				_createTableView(
					HashMapBuilder.put(
						"id", "idRenderer"
					).put(
						"thumbnail", "imageRenderer"
					).build(),
					HashMapBuilder.put(
						"id", "id"
					).put(
						"name", "name"
					).put(
						"thumbnail", "picture"
					).build(),
					HashMapBuilder.put(
						"name", true
					).build(),
					false));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "table"
				).put(
					"default", false
				).put(
					"label", "table"
				).put(
					"name", "table"
				).put(
					"quickActionsEnabled", false
				).put(
					"schema",
					JSONUtil.put(
						"fields",
						JSONUtil.putAll(
							JSONUtil.put(
								"contentRenderer", "imageRenderer"
							).put(
								"contentRendererClientExtension", false
							).put(
								"fieldName", "thumbnail"
							).put(
								"label", "picture"
							).put(
								"localizeLabel", true
							).put(
								"sortable", false
							),
							JSONUtil.put(
								"contentRendererClientExtension", false
							).put(
								"fieldName", "name"
							).put(
								"label", "name"
							).put(
								"localizeLabel", true
							).put(
								"sortable", true
							),
							JSONUtil.put(
								"contentRenderer", "idRenderer"
							).put(
								"contentRendererClientExtension", false
							).put(
								"fieldName", "id"
							).put(
								"label", "id"
							).put(
								"localizeLabel", true
							).put(
								"sortable", false
							)))
				).put(
					"thumbnail", "table"
				)
			).toString(),
			_systemFDSViewSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).toString(),
			JSONCompareMode.LENIENT);

		selectionFilterServiceRegistration.unregister();

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSViewSerializationNoView() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		JSONAssert.assertEquals(
			"[]",
			_systemFDSViewSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).toString(),
			JSONCompareMode.STRICT);

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSViewSerializationSeparateViews() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration1 =
			registerSystemFDSEntry("fdsName1", "/app", "/endpoint", "schema");

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration2 =
			registerSystemFDSEntry("fdsName2", "/app", "/endpoint", "schema");

		ServiceRegistration<FDSView> viewServiceRegistration1 = _registerView(
			"fdsName1",
			_createCardsView(
				"longDescription", "detailURL", "thumbnail", "sticker", "icon",
				"title"));

		ServiceRegistration<FDSView> viewServiceRegistration2 = _registerView(
			"fdsName2",
			_createListView(
				"longDescription", "thumbnail", "sticker", "icon", "title"));

		String viewSerialized1 = _systemFDSViewSerializerImpl.serialize(
			"fdsName1", httpServletRequest
		).toString();

		String viewSerialized2 = _systemFDSViewSerializerImpl.serialize(
			"fdsName2", httpServletRequest
		).toString();

		JSONAssert.assertNotEquals(
			viewSerialized1, viewSerialized2, JSONCompareMode.LENIENT);

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "cards"
				).put(
					"default", false
				).put(
					"label", "cards"
				).put(
					"name", "cards"
				).put(
					"schema",
					JSONUtil.put(
						"description", "longDescription"
					).put(
						"href", "detailURL"
					).put(
						"image", "thumbnail"
					).put(
						"sticker", "sticker"
					).put(
						"symbol", "icon"
					).put(
						"title", "title"
					)
				).put(
					"thumbnail", "cards2"
				)
			).toString(),
			viewSerialized1, JSONCompareMode.LENIENT);

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "list"
				).put(
					"default", false
				).put(
					"label", "list"
				).put(
					"name", "list"
				).put(
					"schema",
					JSONUtil.put(
						"description", "longDescription"
					).put(
						"image", "thumbnail"
					).put(
						"sticker", "sticker"
					).put(
						"symbol", "icon"
					).put(
						"title", "title"
					)
				).put(
					"thumbnail", "list"
				)
			).toString(),
			viewSerialized2, JSONCompareMode.LENIENT);

		viewServiceRegistration1.unregister();

		viewServiceRegistration2.unregister();

		systemFDSEntryServiceRegistration1.unregister();

		systemFDSEntryServiceRegistration2.unregister();
	}

	@Test
	public void testFDSViewSerializationSharingView() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration1 =
			registerSystemFDSEntry("fdsName1", "/app", "/endpoint", "schema");

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration2 =
			registerSystemFDSEntry("fdsName2", "/app", "/endpoint", "schema");

		FDSView cardsView = _createCardsView(
			"longDescription", "detailURL", "thumbnail", "sticker", "icon",
			"title");

		ServiceRegistration<FDSView> cardsViewServiceRegistration1 =
			_registerView("fdsName1", cardsView);

		ServiceRegistration<FDSView> cardsViewServiceRegistration2 =
			_registerView("fdsName2", cardsView);

		JSONAssert.assertEquals(
			_systemFDSViewSerializerImpl.serialize(
				"fdsName1", httpServletRequest
			).toString(),
			_systemFDSViewSerializerImpl.serialize(
				"fdsName2", httpServletRequest
			).toString(),
			JSONCompareMode.STRICT);

		cardsViewServiceRegistration1.unregister();

		cardsViewServiceRegistration2.unregister();

		systemFDSEntryServiceRegistration1.unregister();

		systemFDSEntryServiceRegistration2.unregister();
	}

	/*			@Test
				public void testFDSSelectionFilterWithItemsSerialization()
					throws Exception {
					ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
						registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");
					ServiceRegistration<FDSFilter> selectionFilterServiceRegistration =
						_registerView(
							"fdsName",
							_createSelectionFilter(
								"categoryIds", "By Category",
								FDSEntityFieldTypes.COLLECTION,
								new HashMapBuilder<>().<String, Object>put(
									"exclude", true
								).build(),
								ListUtil.fromArray(
									new SelectionFDSFilterItem("animal", 1),
									new SelectionFDSFilterItem("vegetable", 2)),
								"id", "label", false, true));
					JSONAssert.assertEquals(
						JSONUtil.putAll(
							JSONUtil.put(
								"autocompleteEnabled", false
							).put(
								"entityFieldType", "collection"
							).put(
								"id", "categoryIds"
							).put(
								"items",
								JSONUtil.putAll(
									JSONUtil.put(
										"label", "animal"
									).put(
										"value", 1
									),
									JSONUtil.put(
										"label", "vegetable"
									).put(
										"value", 2
									))
							).put(
								"label", "By Category"
							).put(
								"multiple", true
							).put(
								"preloadedData", JSONUtil.put("exclude", true)
							).put(
								"type", "selection"
							)
						).toString(),
						_systemFDSViewSerializerImpl.serialize(
							"fdsName", httpServletRequest
						).toString(),
						JSONCompareMode.LENIENT);
					selectionFilterServiceRegistration.unregister();
					systemFDSEntryServiceRegistration.unregister();
				}

			*/
	private FDSView _createCardsView(
		String description, String href, String image, String sticker,
		String symbol, String title) {

		return new BaseCardsFDSView() {

			@Override
			public String getDescription() {
				return description;
			}

			@Override
			public String getImage() {
				return image;
			}

			@Override
			public String getLink() {
				return href;
			}

			@Override
			public String getSticker() {
				return sticker;
			}

			@Override
			public String getSymbol() {
				return symbol;
			}

			@Override
			public String getTitle() {
				return title;
			}

		};
	}

	private FDSView _createListView(
		String description, String image, String sticker, String symbol,
		String title) {

		return new BaseListFDSView() {

			@Override
			public String getDescription() {
				return description;
			}

			@Override
			public String getImage() {
				return image;
			}

			@Override
			public String getSticker() {
				return sticker;
			}

			@Override
			public String getSymbol() {
				return symbol;
			}

			@Override
			public String getTitle() {
				return title;
			}

		};
	}

	private FDSView _createTableView(
		Map<String, String> contentRenderers,
		Map<String, String> schemaMappings, Map<String, Boolean> sortableFlags,
		boolean quickActions) {

		return new BaseTableFDSView() {

			@Override
			public FDSTableSchema getFDSTableSchema(Locale locale) {
				FDSTableSchemaBuilder fdsTableSchemaBuilder =
					new FDSTableSchemaBuilderImpl();

				for (String key : schemaMappings.keySet()) {
					fdsTableSchemaBuilder.add(
						key, schemaMappings.get(key),
						fdsTableSchemaField -> {
							if (MapUtil.isNotEmpty(contentRenderers) &&
								contentRenderers.containsKey(key)) {

								fdsTableSchemaField.setContentRenderer(
									contentRenderers.get(key));
							}

							if (MapUtil.isNotEmpty(sortableFlags) &&
								sortableFlags.containsKey(key)) {

								fdsTableSchemaField.setSortable(
									sortableFlags.get(key));
							}
						});
				}

				return fdsTableSchemaBuilder.build();
			}

			@Override
			public boolean isQuickActionsEnabled() {
				return quickActions;
			}

		};
	}

	private ServiceRegistration<FDSView> _registerView(
		String fdsName, FDSView fdsView) {

		return bundleContext.registerService(
			FDSView.class, fdsView,
			MapUtil.singletonDictionary("frontend.data.set.name", fdsName));
	}

	private static final CardsFDSViewContextContributor
		_cardsFDSViewContextContributor = new CardsFDSViewContextContributor();
	private static final FDSViewRegistryImpl _fdsViewRegistryImpl =
		new FDSViewRegistryImpl();
	private static final JSONFactory _jsonFactory = new JSONFactoryImpl();
	private static final ListFDSViewContextContributor
		_listFDSViewContextContributor = new ListFDSViewContextContributor();
	private static final TableFDSViewContextContributor
		_tableFDSViewContextContributor = new TableFDSViewContextContributor();
	private static ServiceTrackerMap
		<String, List<ServiceTrackerCustomizerFactory.ServiceWrapper<FDSView>>>
			_viewServiceTrackerMap;

	private ServiceRegistration<FDSViewContextContributor>
		_cardsFDSViewContextContributorServiceRegistration;
	private final FDSViewContextContributorRegistryImpl
		_fdsViewContextContributorRegistryImpl =
			new FDSViewContextContributorRegistryImpl();
	private ServiceRegistration<FDSViewContextContributor>
		_listFDSViewContextContributorServiceRegistration;
	private final SystemFDSViewSerializerImpl _systemFDSViewSerializerImpl =
		new SystemFDSViewSerializerImpl();
	private ServiceRegistration<FDSViewContextContributor>
		_tableFDSViewContextContributorServiceRegistration;
	private ServiceTrackerMap
		<String,
		 List
			 <ServiceTrackerCustomizerFactory.ServiceWrapper
				 <FDSViewContextContributor>>>
					_viewContextContributorServiceTrackerMap;

}