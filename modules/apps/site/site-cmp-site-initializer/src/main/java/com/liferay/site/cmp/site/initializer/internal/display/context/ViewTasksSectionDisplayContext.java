/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cmp.site.initializer.internal.display.context;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.model.DepotEntryModel;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.frontend.data.set.filter.FDSFilter;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.frontend.data.set.model.FDSActionDropdownItemBuilder;
import com.liferay.frontend.data.set.model.FDSActionDropdownItemList;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenu;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenuBuilder;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.url.builder.PortletURLBuilder;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.RoleService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.workflow.kaleo.model.KaleoTaskInstanceToken;
import com.liferay.site.cmp.site.initializer.internal.constants.CMPActionConstants;
import com.liferay.site.cmp.site.initializer.internal.frontend.data.set.filter.AssigneeSelectionFDSFilter;
import com.liferay.site.cmp.site.initializer.internal.frontend.data.set.filter.CreateDateFDSFilter;
import com.liferay.site.cmp.site.initializer.internal.frontend.data.set.filter.DueDateRangeFDSFilter;
import com.liferay.site.cmp.site.initializer.internal.frontend.data.set.filter.ProjectSelectionFDSFilter;
import com.liferay.site.cmp.site.initializer.internal.frontend.data.set.filter.StateSelectionFDSFilter;
import com.liferay.site.cmp.site.initializer.internal.frontend.data.set.filter.TagSelectionFDSFilter;
import com.liferay.site.cmp.site.initializer.internal.util.ActionUtil;

import jakarta.portlet.ActionRequest;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Gabriel Albuquerque
 */
public class ViewTasksSectionDisplayContext extends BaseSectionDisplayContext {

	public ViewTasksSectionDisplayContext(
		AssetTagLocalService assetTagLocalService,
		ClassNameLocalService classNameLocalService,
		DepotEntryLocalService depotEntryLocalService,
		HttpServletRequest httpServletRequest,
		ObjectDefinition projectObjectDefinition, RoleService roleService,
		ObjectDefinition taskObjectDefinition,
		UserLocalService userLocalService) {

		super(httpServletRequest, taskObjectDefinition);

		_assetTagLocalService = assetTagLocalService;
		_classNameLocalService = classNameLocalService;
		_depotEntryLocalService = depotEntryLocalService;
		_projectObjectDefinition = projectObjectDefinition;
		_roleService = roleService;
		_userLocalService = userLocalService;

		_assetEntry = (AssetEntry)httpServletRequest.getAttribute(
			WebKeys.LAYOUT_ASSET_ENTRY);
	}

	public String getAPIURL() {
		StringBundler sb = new StringBundler(11);

		sb.append("/o/search/v1.0/search?emptySearch=true");

		if (_assetEntry == null) {
			sb.append("&entryClassNames=");
			sb.append(HtmlUtil.escapeURL(objectDefinition.getClassName()));
			sb.append(StringPool.COMMA);
			sb.append(KaleoTaskInstanceToken.class.getName());
		}

		sb.append("&filter=(objectDefinitionId eq ");
		sb.append(objectDefinition.getObjectDefinitionId());

		if (_assetEntry != null) {
			sb.append(" and scopeGroupId eq ");
			sb.append(_assetEntry.getGroupId());
		}
		else {
			sb.append(" or keywords/any(k:startswith(k, '");
			sb.append(objectDefinition.getExternalReferenceCode());
			sb.append("'))");
		}

		sb.append(")&nestedFields=cmpProjectToCMPTasks,embedded");

		return sb.toString();
	}

	public CreationMenu getCreationMenu() {
		return CreationMenuBuilder.addPrimaryDropdownItem(
			dropdownItem -> {
				dropdownItem.putData("action", CMPActionConstants.CREATE_TASK);
				dropdownItem.putData(
					"addProjectURL",
					StringBundler.concat(
						ActionUtil.getAddProjectURL(
							_projectObjectDefinition, themeDisplay),
						"&action=",
						CMPActionConstants.CREATE_PROJECT_GLOBAL_TASK));
				dropdownItem.putData(
					"addTaskURL",
					StringBundler.concat(
						ActionUtil.getAddTaskURL(
							0, objectDefinition, 0, themeDisplay),
						"&action=", CMPActionConstants.CREATE_GLOBAL_TASK));
				dropdownItem.putData(
					"objectDefinitionId",
					String.valueOf(objectDefinition.getObjectDefinitionId()));

				if (_assetEntry != null) {
					dropdownItem.putData(
						"redirect",
						ActionUtil.getAddTaskURL(
							_assetEntry.getGroupId(), objectDefinition,
							_assetEntry.getClassPK(), themeDisplay));
				}

				dropdownItem.putData(
					"title",
					objectDefinition.getLabel(themeDisplay.getLocale()));
				dropdownItem.setIcon("forms");
				dropdownItem.setLabel(
					LanguageUtil.get(
						httpServletRequest,
						(_assetEntry == null) ? "new" : "new-task"));
			}
		).build();
	}

	public Map<String, Object> getEmptyState() {
		return HashMapBuilder.<String, Object>put(
			"description",
			LanguageUtil.get(
				httpServletRequest, "click-new-to-create-your-first-task")
		).put(
			"image", "/states/cmp_empty_state_tasks.svg"
		).put(
			"title", LanguageUtil.get(httpServletRequest, "no-tasks-yet")
		).build();
	}

	public List<FDSActionDropdownItem> getFDSActionDropdownItems() {
		return FDSActionDropdownItemList.of(
			FDSActionDropdownItemBuilder.setFDSActionDropdownItems(
				FDSActionDropdownItemList.of(
					FDSActionDropdownItemBuilder.setTarget(
						"modal-workflow-transition"
					).build(
						"workflow-transition"
					))
			).setType(
				"group"
			).build(
				"workflow-transitions"
			),
			FDSActionDropdownItemBuilder.setFDSActionDropdownItems(
				FDSActionDropdownItemList.of(
					FDSActionDropdownItemBuilder.setHref(
						StringBundler.concat(
							ActionUtil.getBaseEditTaskURL(
								objectDefinition, themeDisplay),
							"{embedded.id}?redirect=",
							themeDisplay.getURLCurrent())
					).setIcon(
						"pencil"
					).setLabel(
						LanguageUtil.get(httpServletRequest, "edit")
					).setMethod(
						"get"
					).setPermissionKey(
						"update"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"entryClassName", objectDefinition.getClassName()
						).build()
					).build(
						"edit"
					),
					FDSActionDropdownItemBuilder.setHref(
						StringBundler.concat(
							ActionUtil.getBaseViewTaskURL(
								objectDefinition, themeDisplay),
							"{embedded.id}?redirect=",
							themeDisplay.getURLCurrent())
					).setIcon(
						"view"
					).setLabel(
						LanguageUtil.get(httpServletRequest, "view")
					).setMethod(
						"get"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"entryClassName", objectDefinition.getClassName()
						).build()
					).build(
						"actionLink"
					),
					FDSActionDropdownItemBuilder.setLabel(
						LanguageUtil.get(httpServletRequest, "assign-to-...")
					).setPermissionKey(
						"get"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"entryClassName", objectDefinition.getClassName()
						).build()
					).build(
						"assign-to"
					),
					FDSActionDropdownItemBuilder.setIcon(
						"trash"
					).setLabel(
						LanguageUtil.get(httpServletRequest, "delete")
					).setPermissionKey(
						"delete"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"entryClassName", objectDefinition.getClassName()
						).build()
					).build(
						"delete"
					),
					FDSActionDropdownItemBuilder.setHref(
						PortletURLBuilder.create(
							PortalUtil.getControlPanelPortletURL(
								httpServletRequest,
								PortletKeys.MY_WORKFLOW_TASK,
								ActionRequest.RENDER_PHASE)
						).setMVCPath(
							"/edit_workflow_task.jsp"
						).setRedirect(
							themeDisplay.getURLCurrent()
						).setParameter(
							"workflowTaskId", "{embedded.id}"
						).buildString()
					).setIcon(
						"view"
					).setLabel(
						LanguageUtil.get(httpServletRequest, "view")
					).setPermissionKey(
						"get"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"entryClassName",
							KaleoTaskInstanceToken.class.getName()
						).build()
					).build(
						"actionLinkWorkflowTask"
					),
					FDSActionDropdownItemBuilder.setLabel(
						LanguageUtil.get(httpServletRequest, "assign-to-me")
					).setPermissionKey(
						"assignToMe"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"embedded.assignedToMe", false
						).put(
							"embedded.completed", false
						).put(
							"entryClassName",
							KaleoTaskInstanceToken.class.getName()
						).build()
					).build(
						"assignToMeWorkflowTask"
					),
					FDSActionDropdownItemBuilder.setLabel(
						LanguageUtil.get(httpServletRequest, "assign-to-...")
					).setPermissionKey(
						"assignToUser"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"embedded.completed", false
						).put(
							"entryClassName",
							KaleoTaskInstanceToken.class.getName()
						).build()
					).build(
						"assignToWorkflowTask"
					),
					FDSActionDropdownItemBuilder.setIcon(
						"date-time"
					).setLabel(
						LanguageUtil.get(httpServletRequest, "update-due-date")
					).setPermissionKey(
						"updateDueDate"
					).setVisibilityFilters(
						HashMapBuilder.<String, Object>put(
							"embedded.completed", false
						).put(
							"entryClassName",
							KaleoTaskInstanceToken.class.getName()
						).build()
					).build(
						"updateDueDateWorkflowTask"
					))
			).setSeparator(
				true
			).setType(
				"group"
			).build(
				"other-actions"
			));
	}

	public List<FDSFilter> getFDSFilters() {
		List<FDSFilter> fdsFilters = new ArrayList<>();

		long[] groupIds = null;

		if (_assetEntry != null) {
			groupIds = new long[] {_assetEntry.getGroupId()};
		}
		else {
			groupIds = TransformUtil.transformToLongArray(
				_depotEntryLocalService.getDepotEntries(
					_projectObjectDefinition.getCompanyId(),
					DepotConstants.TYPE_PROJECT),
				DepotEntryModel::getGroupId);
		}

		fdsFilters.add(
			new AssigneeSelectionFDSFilter(
				_classNameLocalService, _projectObjectDefinition.getCompanyId(),
				groupIds, _roleService, _userLocalService));

		fdsFilters.add(new CreateDateFDSFilter());
		fdsFilters.add(new DueDateRangeFDSFilter());

		if (_assetEntry == null) {
			fdsFilters.add(
				new ProjectSelectionFDSFilter(_projectObjectDefinition));
		}

		fdsFilters.add(new StateSelectionFDSFilter());
		fdsFilters.add(
			new TagSelectionFDSFilter(_assetTagLocalService, groupIds));

		return fdsFilters;
	}

	public Map<String, Object> getTasksQuickFiltersProperties() {
		return HashMapBuilder.<String, Object>put(
			"projectId",
			() -> {
				if (_assetEntry == null) {
					return null;
				}

				return _assetEntry.getClassPK();
			}
		).build();
	}

	private final AssetEntry _assetEntry;
	private final AssetTagLocalService _assetTagLocalService;
	private final ClassNameLocalService _classNameLocalService;
	private final DepotEntryLocalService _depotEntryLocalService;
	private final ObjectDefinition _projectObjectDefinition;
	private final RoleService _roleService;
	private final UserLocalService _userLocalService;

}