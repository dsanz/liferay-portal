/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set.action;

import com.liferay.frontend.data.set.action.FDSItemsActions;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;
import com.liferay.site.cms.site.initializer.internal.display.context.ViewAllSectionDisplayContext;
import com.liferay.site.cms.site.initializer.internal.fragment.renderer.BaseJSPSectionFragmentRenderer;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + CMSSiteInitializerFDSNames.ALL_SECTION,
	service = FDSItemsActions.class
)
public class AllSectionFDSItemsActions implements FDSItemsActions {

	@Override
	public List<FDSActionDropdownItem> getFDSActionDropdownItems(
		HttpServletRequest httpServletRequest) {

		try {
			ViewAllSectionDisplayContext viewAllSectionDisplayContext =
				_baseJSPSectionFragmentRenderer.getDisplayContext(
					httpServletRequest);

			return viewAllSectionDisplayContext.getFDSActionDropdownItems();
		}
		catch (PortalException portalException) {
			_log.error("Unable to get items actions", portalException);
		}

		return Collections.emptyList();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AllSectionFDSItemsActions.class);

	@Reference(
		target = "(frontend.data.set.name=" + CMSSiteInitializerFDSNames.ALL_SECTION + ")"
	)
	private BaseJSPSectionFragmentRenderer<ViewAllSectionDisplayContext>
		_baseJSPSectionFragmentRenderer;

}