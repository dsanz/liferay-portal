/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */


package com.liferay.frontend.data.set.internal.creator;

import com.liferay.frontend.data.set.DataSet;
import com.liferay.frontend.data.set.DataSetRegistry;
import com.liferay.frontend.data.set.creator.DataSetObjectEntryCreator;
import com.liferay.frontend.data.set.filter.BaseClientExtensionFDSFilter;
import com.liferay.frontend.data.set.filter.BaseDateRangeFDSFilter;
import com.liferay.frontend.data.set.filter.BaseSelectionFDSFilter;
import com.liferay.frontend.data.set.filter.FDSFilter;
import com.liferay.frontend.data.set.filter.FDSFilterContextContributorRegistry;
import com.liferay.frontend.data.set.filter.FDSFilterRegistry;
import com.liferay.frontend.data.set.view.FDSView;
import com.liferay.frontend.data.set.view.FDSViewContextContributorRegistry;
import com.liferay.frontend.data.set.view.FDSViewRegistry;
import com.liferay.frontend.data.set.view.table.BaseTableFDSView;
import com.liferay.frontend.data.set.view.table.FDSTableSchema;
import com.liferay.frontend.data.set.view.table.FDSTableSchemaField;
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
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author Daniel Sanz
 */
@Component(immediate = true, service = DataSetObjectEntryCreator.class)
public class DataSetObjectEntryCreatorImpl implements
	DataSetObjectEntryCreator {
	@Override
	public void create(
		String datasetERC, HttpServletRequest httpServletRequest) {
		
		DataSet dataSet = _dataSetRegistry.getDataSet(datasetERC);

		if (dataSet == null) {
			return;
		}

		ObjectDefinition objectDefinition = _objectDefinitionLocalService.fetchObjectDefinitionByExternalReferenceCode(
			"L_DATA_SET", _portal.getCompanyId(httpServletRequest));

		ObjectEntry fdsObjectEntry;

		Locale locale = _portal.getLocale(httpServletRequest);

		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", locale, getClass());

		try {
			fdsObjectEntry = _objectEntryService.addObjectEntry(
				0, objectDefinition.getObjectDefinitionId(),
				HashMapBuilder.<String, Serializable>put(
					"externalReferenceCode", datasetERC
				).put(
					"label",dataSet.getName()        // TODO: Allow label localization for all locales
				)
				.put(
					"name", datasetERC
				).put(
					"restApplication", dataSet.getRESTApplication()
				).put(
					"restEndpoint", dataSet.getRESTEndpoint()
				).put(
					"restSchema", dataSet.getRESTSchema()
				).put(
					"additionalAPIURLParameters", dataSet.getAdditionalAPIURLParameters()
				).put(
					"listOfItemsPerPage", "12,20"
				).put(
					"defaultItemsPerPage", 12
				).build(),
				new ServiceContext());

			List<FDSFilter> fdsFilters = _fdsFilterRegistry.getFDSFilters(datasetERC);

			for (FDSFilter fdsFilter : fdsFilters) {
				/*JSONObject jsonObject = JSONUtil.put(
					"entityFieldType", fdsFilter.getEntityFieldType()
				).put(
					"id", fdsFilter.getId()
				).put(
					"label", _language.get(resourceBundle, fdsFilter.getLabel())
				).put(
					"preloadedData", fdsFilter.getPreloadedData()
				).put(
					"type", fdsFilter.getType()
				);

				List<FDSFilterContextContributor> fdsFilterContextContributors =
					_fdsFilterContextContributorRegistry.
						getFDSFilterContextContributors(fdsFilter.getType());

				for (FDSFilterContextContributor fdsFilterContextContributor :
						fdsFilterContextContributors) {

					Map<String, Object> fdsFilterContext =
						fdsFilterContextContributor.getFDSFilterContext(
							fdsFilter, locale);

					if (fdsFilterContext == null) {
						continue;
					}

					for (Map.Entry<String, Object> entry :
							fdsFilterContext.entrySet()) {

						jsonObject.put(entry.getKey(), entry.getValue());
					}
				}
				  */
				JSONObject preloadedData = JSONUtil.put("preloadedData", fdsFilter.getPreloadedData());

				HashMapBuilder.HashMapWrapper<String, Serializable> values =
					HashMapBuilder.<String, Serializable>put(
						"externalReferenceCode", StringBundler.concat(fdsFilter.getId(), "_",
							RandomUtil.nextInts(0, 5))
					).put("entityFieldType", fdsFilter.getEntityFieldType()
					).put(
						"fieldName", fdsFilter.getId()
					).put(
						"label_i18n", HashMapBuilder.put(
								"en_US",
								fdsFilter.getLabel())
							.build()
					).put(
						"type", fdsFilter.getType()
					);

				String filterObjectDefinitionERC = StringPool.BLANK;
				String filterFDSEntryRelationshipName = StringPool.BLANK;

				if (fdsFilter instanceof BaseDateRangeFDSFilter) {
					filterObjectDefinitionERC = "L_DATA_SET_DATE_FILTER";

					filterFDSEntryRelationshipName = "r_dataSetToDataSetDateFilters_l_dataSetId";

					values.put(
						"from", preloadedData.getString("from")
					).put(
						"to", preloadedData.getString("to")
					);
				}
				else if (fdsFilter instanceof BaseSelectionFDSFilter) {
					BaseSelectionFDSFilter selectionFdsFilter = (BaseSelectionFDSFilter) fdsFilter;

					filterObjectDefinitionERC = "L_DATA_SET_SELECTION_FILTER";

					filterFDSEntryRelationshipName = "r_dataSetToDataSetSelectionFilters_l_dataSetId";

					values.put(
						"include", true
					).put(
						"multiple", selectionFdsFilter.isMultiple()
					).put(
//						"preselectedValues", selectionFdsFilter.getPreloadedData() ??  // TODO: handle preloaded data here
//					).put(
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
						"sourceType", "API_REST_APPLICATION"  // TODO: support picklist for fdsFilter.getSelectionFDSFilterItems()
					).put(
						"itemLabel", selectionFdsFilter.getItemLabel()
					)
					;
					/* missing from BaseSelectionFDSFilter
						getPlaceholder()
						isAutocompleteEnabled()
					 */
				}
				else if (fdsFilter instanceof BaseClientExtensionFDSFilter) {
					filterObjectDefinitionERC = "L_DATA_SET_CLIENT_EXTENSION_FILTER";

					filterFDSEntryRelationshipName = "r_dataSetToDataSetClientExtensionFilters_l_dataSetId";

					// TODO: map getModuleURL()
				}

				values.put(filterFDSEntryRelationshipName, fdsObjectEntry.getObjectEntryId());

				ObjectDefinition filterObjectDefinition = _objectDefinitionLocalService.fetchObjectDefinitionByExternalReferenceCode(
					filterObjectDefinitionERC, _portal.getCompanyId(httpServletRequest));

				ObjectEntry fdsFilterObjectEntry = _objectEntryService.addObjectEntry(
					0, filterObjectDefinition.getObjectDefinitionId(),
						values.build(),
						new ServiceContext());
			}

			// Table sections

			ObjectDefinition tableSectionObjectDefinition =
				_objectDefinitionLocalService.fetchObjectDefinitionByExternalReferenceCode(
					"L_DATA_SET_TABLE_SECTION", _portal.getCompanyId(httpServletRequest));

			List<FDSView> fdsViews = _fdsViewRegistry.getFDSViews(datasetERC);

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

				BaseTableFDSView baseTableFDSView = (BaseTableFDSView) fdsView;

				FDSTableSchema fdsTableSchema = baseTableFDSView.getFDSTableSchema(
					locale);

				Map<String, FDSTableSchemaField> fieldsMap =
					fdsTableSchema.getFDSTableSchemaFieldsMap();

			//	ResourceBundle resourceBundle = baseTableFDSView.getResourceBundle(	locale);

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
							"externalReferenceCode", StringBundler.concat(fdsTableSchemaField.getFieldName(), "_",
								RandomUtil.nextInts(0, 5))
						).put(
							"fieldName", fdsTableSchemaField.getFieldName()
						).put(
							"label_i18n",
							HashMapBuilder.put(
								"en_US",
								fdsTableSchemaField.getLabel())
											.build()
						).put(
							"renderer", Validator.isNotNull(fdsTableSchemaField.getContentRenderer()) ? fdsTableSchemaField.getContentRenderer() : "default"
						).put(
							"sortable", fdsTableSchemaField.isSortable()
						).put(   // we don't have the type (mandatory). Not easy to guess, we'll need to inform it.
							"type", "string"
						).put(
							"r_dataSetToDataSetTableSections_l_dataSetId", fdsObjectEntry.getObjectEntryId()
						);

					ObjectEntry fdsFieldObjectEntry = _objectEntryService.addObjectEntry(
						0, tableSectionObjectDefinition.getObjectDefinitionId(),
						values.build(), new ServiceContext());

				}

				/* missing from baseTableFDSView
					baseTableFDSView.isQuickActionsEnabled()

					missing from object:
					rendererType
				 */
				//	"name", "type","renderer", "rendererType" "sortable", "label"
			}
		}
		catch (PortalException e) {
			throw new RuntimeException(e);
		}

	}

	@Reference
	private DataSetRegistry _dataSetRegistry;

	@Reference
	private FDSFilterRegistry _fdsFilterRegistry;

	@Reference
	private FDSFilterContextContributorRegistry
		_fdsFilterContextContributorRegistry;

	@Reference
	private FDSViewContextContributorRegistry
		_fdsViewContextContributorRegistry;

	@Reference
	private FDSViewRegistry _fdsViewRegistry;

	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

	@Reference
	private ObjectEntryService _objectEntryService;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;
}