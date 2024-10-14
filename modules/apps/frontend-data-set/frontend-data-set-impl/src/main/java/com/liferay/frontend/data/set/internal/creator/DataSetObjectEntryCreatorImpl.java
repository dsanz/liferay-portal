/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */


package com.liferay.frontend.data.set.internal.creator;

import com.liferay.frontend.data.set.DataSet;
import com.liferay.frontend.data.set.DataSetRegistry;
import com.liferay.frontend.data.set.action.FDSCreationMenu;
import com.liferay.frontend.data.set.action.FDSCreationMenuRegistry;
import com.liferay.frontend.data.set.action.FDSItemActionList;
import com.liferay.frontend.data.set.action.FDSItemActionListRegistry;
import com.liferay.frontend.data.set.creator.DataSetObjectEntryCreator;
import com.liferay.frontend.data.set.filter.BaseClientExtensionFDSFilter;
import com.liferay.frontend.data.set.filter.BaseDateRangeFDSFilter;
import com.liferay.frontend.data.set.filter.BaseSelectionFDSFilter;
import com.liferay.frontend.data.set.filter.FDSFilter;
import com.liferay.frontend.data.set.filter.FDSFilterRegistry;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.frontend.data.set.view.FDSView;
import com.liferay.frontend.data.set.view.FDSViewRegistry;
import com.liferay.frontend.data.set.view.table.BaseTableFDSView;
import com.liferay.frontend.data.set.view.table.FDSTableSchema;
import com.liferay.frontend.data.set.view.table.FDSTableSchemaField;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenu;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.security.RandomUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(immediate = true, service = DataSetObjectEntryCreator.class)
public class DataSetObjectEntryCreatorImpl implements
	DataSetObjectEntryCreator {
	@Override
	public void create(
		String datasetERC, HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {

		DataSet dataSet = _dataSetRegistry.getDataSet(datasetERC);

		if (dataSet == null) {
			return;
		}

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					"L_DATA_SET", _portal.getCompanyId(httpServletRequest));

		try {
			ObjectEntry fdsObjectEntry = _objectEntryService.addObjectEntry(
				0, objectDefinition.getObjectDefinitionId(),
				HashMapBuilder.<String, Serializable>put(

					// TODO: Allow label localization for all locales

					"label", dataSet.getName()
				).put(
					"additionalAPIURLParameters",
					dataSet.getAdditionalAPIURLParameters()
				)
				.put(
					"defaultItemsPerPage", 12
				).put(
					"externalReferenceCode", datasetERC
				).put(
					"listOfItemsPerPage", "12,20"
				).put(
					"name", datasetERC
				).put(
					"restApplication", dataSet.getRESTApplication()
				).put(
					"restEndpoint", dataSet.getRESTEndpoint()
				).put(
					"restSchema", dataSet.getRESTSchema()
				).build(),
				new ServiceContext());

			_createFilters(datasetERC, fdsObjectEntry, httpServletRequest);

			_createTableSections(
				datasetERC, fdsObjectEntry, httpServletRequest);

			_createItemActions(
				datasetERC, fdsObjectEntry, httpServletRequest,
				httpServletResponse);

			_createCreationMenu(
				datasetERC, fdsObjectEntry, httpServletRequest,
				httpServletResponse);
		}
		catch (PortalException portalException) {
			throw new RuntimeException(portalException);
		}
	}

	private void _createCreationMenu(
		String datasetERC, ObjectEntry fdsObjectEntry,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse)
	throws PortalException {
		FDSCreationMenu fdsCreationMenu =
			_fdsCreationMenuRegistry.getFDSCreationMenu(datasetERC);

		if (fdsCreationMenu == null) {
			return;
		}

		CreationMenu creationMenu = fdsCreationMenu.getCreationMenu(
			httpServletRequest, httpServletResponse);

		if (creationMenu.isEmpty()) {
			return;
		}

		ObjectDefinition actionObjectDefinition =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					"L_DATA_SET_ACTION",
					_portal.getCompanyId(httpServletRequest));

		List<DropdownItem> dropdownItems =
			(List<DropdownItem>)creationMenu.get("primaryItems");

		if (fdsCreationMenu.isProxy()) {
			for (DropdownItem dropdownItem : dropdownItems) {
				Map<String, Object> data =
					(Map<String, Object>)dropdownItem.get("data");

				if (data == null) {
					data = new HashMap<>();
				}

				_objectEntryService.addObjectEntry(
					0,
					actionObjectDefinition.getObjectDefinitionId(),
					HashMapBuilder.<String, Serializable>put(

						// TODO: handle this new type where we delegate
						//  rendering to the back end

						"type", "proxy"
					).put(
						"externalReferenceCode",
						String.valueOf(data.get("id"))
					).put(
						"icon",
						String.valueOf(dropdownItem.get("icon"))
					).put(
						"label_i18n", HashMapBuilder.put(
								"en_US",
								StringBundler.concat("Proxy for ",
									dropdownItem.get("label"),
									"item action")
						).build()
					).put(
						"r_dataSetToCreationDataSetActions_l_dataSetId",
						fdsObjectEntry.getObjectEntryId()
					).put(
						"url", StringBundler.concat("proxy://",
							datasetERC, "/",
							fdsCreationMenu.getClass().getName(), "/",
							data.get("id"))
					).build(), new ServiceContext());
			}
		}
		else {
			for (DropdownItem dropdownItem : dropdownItems) {

				Map<String, Object> data =
					(Map<String, Object>)dropdownItem.get("data");

				if (data == null) {
					data = new HashMap<>();
				}

				String type = "link";

				if (Validator.isNotNull(
						String.valueOf(dropdownItem.get("target")))) {

					type = String.valueOf(dropdownItem.get("target"));
				}

				String permissionKey = String.valueOf(
					data.get("permissionKey"));

				_objectEntryService.addObjectEntry(
					0, actionObjectDefinition.getObjectDefinitionId(),
					HashMapBuilder.<String, Serializable>put(
						"confirmationMessage",
						String.valueOf(data.get("confirmationMessage"))
					).put(
						"icon",
						String.valueOf(dropdownItem.get("icon"))
					).put(
						"label_i18n",
						HashMapBuilder.put(
							"en_US",
							String.valueOf(dropdownItem.get("label"))
						).build()
					).put(
						"method", String.valueOf(data.get("method"))
					).put(
						"permissionKey",
						() -> {
							if (Validator.isNotNull(permissionKey)) {
								return permissionKey;
							}

							return StringPool.BLANK;
							}
						).put(
							"r_dataSetToCreationDataSetActions_l_dataSetId",
							fdsObjectEntry.getObjectEntryId()
						).put(
							"type", type

							// TODO: target=event not supported by DSM

						).put(
							"url",
							String.valueOf(dropdownItem.get("href"))

							// TODO: trim server + port + context path info
							// TODO: trim p_p_auth parameter

						).build(), new ServiceContext());

				/*
				From object definition:
					confirmationMessage, confirmationMessageType,
					errorMessage
					icon
					label
					method
					modalSize
					permissionKey
					requestBody
					successMessage
					title
					type
					url
				 */
			}
		}

	}

	private void _createFilters(
			String datasetERC, ObjectEntry fdsObjectEntry,
			HttpServletRequest httpServletRequest)
		throws PortalException {

		List<FDSFilter> fdsFilters = _fdsFilterRegistry.getFDSFilters(
			datasetERC);

		if (ListUtil.isEmpty(fdsFilters)) {
			return;
		}

		for (FDSFilter fdsFilter : fdsFilters) {
			JSONObject preloadedDataJSONObject = JSONUtil.put(
				"preloadedData", fdsFilter.getPreloadedData());

			HashMapBuilder.HashMapWrapper<String, Serializable> values =
				HashMapBuilder.<String, Serializable>put(
					"entityFieldType", fdsFilter.getEntityFieldType()
				).put(
					"externalReferenceCode",
					StringBundler.concat(
						fdsFilter.getId(), "_", RandomUtil.nextInts(0, 5))
				).put(
					"fieldName", fdsFilter.getId()
				).put(
					"label_i18n",
					HashMapBuilder.put(
						"en_US", fdsFilter.getLabel()
					).build()
				).put(
					"type", fdsFilter.getType()
				);

			String filterObjectDefinitionERC = StringPool.BLANK;
			String filterFDSEntryRelationshipName = StringPool.BLANK;

			if (fdsFilter instanceof BaseDateRangeFDSFilter) {
				filterObjectDefinitionERC = "L_DATA_SET_DATE_FILTER";

				filterFDSEntryRelationshipName =
					"r_dataSetToDataSetDateFilters_l_dataSetId";

				values.put(
					"from", preloadedDataJSONObject.getString("from")
				).put(
					"to", preloadedDataJSONObject.getString("to")
				);
			}
			else if (fdsFilter instanceof BaseSelectionFDSFilter) {
				BaseSelectionFDSFilter selectionFdsFilter =
					(BaseSelectionFDSFilter)fdsFilter;

				filterObjectDefinitionERC = "L_DATA_SET_SELECTION_FILTER";

				filterFDSEntryRelationshipName =
					"r_dataSetToDataSetSelectionFilters_l_dataSetId";

				values.put(
					"include", true
				).put(
					"multiple", selectionFdsFilter.isMultiple()
				).put(

					// TODO: handle preloaded data here

//					"preselectedValues", selectionFdsFilter.getPreloadedData()
//				).put(
					"itemKey", selectionFdsFilter.getItemKey()
				).put(
					"itemLabel", selectionFdsFilter.getItemLabel()
//					).put(
//						"restApplication", selectionFdsFilter.?
//					).put(
//						"restEndpoint", selectionFdsFilter.?
//					).put(
//						"restSchema", selectionFdsFilter.?
				).put(
					"source", selectionFdsFilter.getAPIURL()
				).put(

					// TODO: support picklist for
					//  fdsFilter.getSelectionFDSFilterItems()

					"sourceType", "API_REST_APPLICATION"
				).put(
					"itemLabel", selectionFdsFilter.getItemLabel()
				);
				/* missing from BaseSelectionFDSFilter
					getPlaceholder()
					isAutocompleteEnabled()
				 */
			}
			else if (fdsFilter instanceof BaseClientExtensionFDSFilter) {
				filterObjectDefinitionERC =
					"L_DATA_SET_CLIENT_EXTENSION_FILTER";

				filterFDSEntryRelationshipName =
					"r_dataSetToDataSetClientExtensionFilters_l_dataSetId";

				// TODO: map getModuleURL()

			}

			values.put(
				filterFDSEntryRelationshipName,
				fdsObjectEntry.getObjectEntryId());

			ObjectDefinition filterObjectDefinition =
				_objectDefinitionLocalService.
					fetchObjectDefinitionByExternalReferenceCode(
						filterObjectDefinitionERC,
						_portal.getCompanyId(httpServletRequest));

			_objectEntryService.addObjectEntry(
				0, filterObjectDefinition.getObjectDefinitionId(),
				values.build(), new ServiceContext());
		}
	}

	private void _createItemActions(
			String datasetERC, ObjectEntry fdsObjectEntry,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		FDSItemActionList fdsItemActionList =
			_fdsItemActionListRegistry.getFDSItemActionList(datasetERC);

		if (fdsItemActionList == null) {
			return;
		}

		ObjectDefinition actionObjectDefinition =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					"L_DATA_SET_ACTION",
					_portal.getCompanyId(httpServletRequest));

		if (fdsItemActionList.isProxy()) {
			for (FDSActionDropdownItem fdsActionDropdownItem :
					fdsItemActionList.getDropdownItems(
						httpServletRequest, httpServletResponse)) {

				Map<String, Object> data =
					(Map<String, Object>)fdsActionDropdownItem.get("data");

				if (data == null) {
					data = new HashMap<>();
				}

				_objectEntryService.addObjectEntry(
					0,
					actionObjectDefinition.getObjectDefinitionId(),
					HashMapBuilder.<String, Serializable>put(

						// TODO: handle this new type where we delegate
						//  rendering to the back end

						"type", "proxy"
					).put(
						"externalReferenceCode",
						String.valueOf(data.get("id"))
					).put(
						"icon",
						String.valueOf(fdsActionDropdownItem.get("icon"))
					).put(
						"label_i18n", HashMapBuilder.put(
								"en_US",
								StringBundler.concat("Proxy for ",
									fdsActionDropdownItem.get("label"),
									"item action")
						).build()
					).put(
						"r_dataSetToItemDataSetActions_l_dataSetId",
						fdsObjectEntry.getObjectEntryId()
					).put(
						"url", StringBundler.concat("proxy://",
							datasetERC, "/",
							fdsItemActionList.getClass().getName(), "/",
							data.get("id"))
					).build(), new ServiceContext());
			}
		}
		else {
			for (FDSActionDropdownItem fdsActionDropdownItem :
					fdsItemActionList.getDropdownItems(
						httpServletRequest, httpServletResponse)) {

				Map<String, Object> data =
					(Map<String, Object>)fdsActionDropdownItem.get("data");

				if (data == null) {
					data = new HashMap<>();
				}

				String type = "link";

				if (Validator.isNotNull(
						String.valueOf(fdsActionDropdownItem.get("target")))) {

					type = String.valueOf(fdsActionDropdownItem.get("target"));
				}

				String permissionKey = String.valueOf(
					data.get("permissionKey"));

				_objectEntryService.addObjectEntry(
					0, actionObjectDefinition.getObjectDefinitionId(),
					HashMapBuilder.<String, Serializable>put(
						"confirmationMessage",
						String.valueOf(data.get("confirmationMessage"))
					).put(
						"icon",
						String.valueOf(fdsActionDropdownItem.get("icon"))
					).put(
						"label_i18n",
						HashMapBuilder.put(
							"en_US",
							String.valueOf(fdsActionDropdownItem.get("label"))
						).build()
					).put(
						"method", String.valueOf(data.get("method"))
					).put(
						"permissionKey",
						() -> {
							if (Validator.isNotNull(permissionKey)) {
								return permissionKey;
							}

							return StringPool.BLANK;
							}
						).put(
							"r_dataSetToItemDataSetActions_l_dataSetId",
							fdsObjectEntry.getObjectEntryId()
						).put(
							"type", type

							// TODO: target=event not supported by DSM

						).put(
							"url",
							String.valueOf(fdsActionDropdownItem.get("href"))

							// TODO: trim server + port + context path info
							// TODO: trim p_p_auth parameter

						).build(), new ServiceContext());

				/*
				From object definition:
					confirmationMessage, confirmationMessageType,
					errorMessage
					icon
					label
					method
					modalSize
					permissionKey
					requestBody
					successMessage
					title
					type
					url
				 */
			}
		}
	}

	private void _createTableSections(
			String datasetERC, ObjectEntry fdsObjectEntry,
			HttpServletRequest httpServletRequest)
		throws PortalException {

		List<FDSView> fdsViews = _fdsViewRegistry.getFDSViews(datasetERC);

		if (ListUtil.isEmpty(fdsViews)) {
			return;
		}

		ObjectDefinition tableSectionObjectDefinition =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					"L_DATA_SET_TABLE_SECTION",
					_portal.getCompanyId(httpServletRequest));

		Locale locale = _portal.getLocale(httpServletRequest);

		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", locale, getClass());

		for (FDSView fdsView : fdsViews) {
			if (!(fdsView instanceof BaseTableFDSView)) {
				continue;
			}

			// General Viz Mode info (not supported in DSM yet)

			/*
			JSONObject jsonObject = JSONUtil.put(
				"contentRenderer", fdsView.getContentRenderer()
			).put(
				"contentRendererModuleURL",
				fdsView.getContentRendererModuleURL()
			).put(
				"default", fdsView.isDefault()
			).put(
				"label",
				_language.get(
					ResourceBundleUtil.getBundle(
						"content.Language", locale, getClass()),
					fdsView.getLabel())
			).put(
				"name", fdsView.getName()
			).put(
				"thumbnail", fdsView.getThumbnail()
			);*/

			BaseTableFDSView baseTableFDSView = (BaseTableFDSView)fdsView;

			FDSTableSchema fdsTableSchema = baseTableFDSView.getFDSTableSchema(
				locale);

			Map<String, FDSTableSchemaField> fieldsMap =
				fdsTableSchema.getFDSTableSchemaFieldsMap();

			for (FDSTableSchemaField fdsTableSchemaField : fieldsMap.values()) {
				String label = fdsTableSchemaField.getLabel();

				if (fdsTableSchemaField.isLocalizeLabel()) {
					label = _language.get(
						resourceBundle, fdsTableSchemaField.getLabel());
				}

				if (Validator.isNull(label)) {
					label = StringPool.BLANK;
				}

				HashMapBuilder.HashMapWrapper<String, Serializable> values =
					HashMapBuilder.<String, Serializable>put(

						// we don't have the type (mandatory).
						// Not easy to guess, we'll need to inform it.

						"type", "string"
					).put(
						"externalReferenceCode",
						StringBundler.concat(fdsTableSchemaField.getFieldName(),
							"_",
							RandomUtil.nextInts(0, 5))
					).put(
						"fieldName",
						StringUtil.removeLast(
							fdsTableSchemaField.getFieldName(), ".LANG")
					).put(
						"label_i18n",
						HashMapBuilder.put("en_US", label).build()
					).put(
						"r_dataSetToDataSetTableSections_l_dataSetId",
						fdsObjectEntry.getObjectEntryId()
					).put(
						"renderer", () -> {
							if (Validator.isNotNull(
									fdsTableSchemaField.getContentRenderer())) {

								return fdsTableSchemaField.getContentRenderer();
							}

							return "default";
						}
					).put(
						"sortable", fdsTableSchemaField.isSortable()
					);

				_objectEntryService.addObjectEntry(
					0, tableSectionObjectDefinition.getObjectDefinitionId(),
					values.build(), new ServiceContext());
			}

			/* missing from baseTableFDSView
				baseTableFDSView.isQuickActionsEnabled()

				missing from object:
				rendererType
			 */
		}
	}

	@Reference
	private DataSetRegistry _dataSetRegistry;

	@Reference
	private FDSCreationMenuRegistry _fdsCreationMenuRegistry;

	@Reference
	private FDSFilterRegistry _fdsFilterRegistry;

	@Reference
	private FDSItemActionListRegistry _fdsItemActionListRegistry;

	@Reference
	private FDSViewRegistry _fdsViewRegistry;

	@Reference
	private Language _language;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryService _objectEntryService;

	@Reference
	private Portal _portal;

}