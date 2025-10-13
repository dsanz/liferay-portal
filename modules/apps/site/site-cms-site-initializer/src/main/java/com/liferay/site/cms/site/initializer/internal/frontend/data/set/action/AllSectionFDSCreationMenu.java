/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set.action;

import com.liferay.frontend.data.set.FDSEntryItemImportPolicy;
import com.liferay.frontend.data.set.action.FDSCreationMenu;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenu;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.site.cms.site.initializer.internal.constants.CMSSiteInitializerFDSNames;
import com.liferay.site.cms.site.initializer.internal.display.context.ViewAllSectionDisplayContext;
import com.liferay.site.cms.site.initializer.internal.fragment.renderer.BaseJSPSectionFragmentRenderer;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + CMSSiteInitializerFDSNames.ALL_SECTION,
	service = FDSCreationMenu.class
)
public class AllSectionFDSCreationMenu implements FDSCreationMenu {

	@Override
	public CreationMenu getCreationMenu(HttpServletRequest httpServletRequest) {
		try {
			ViewAllSectionDisplayContext viewAllSectionDisplayContext =
				_baseJSPSectionFragmentRenderer.getDisplayContext(
					httpServletRequest);

			return viewAllSectionDisplayContext.getCreationMenu();
		}
		catch (PortalException portalException) {
			_log.error("Unable to get creation menu", portalException);
		}

		return new CreationMenu();
	}

	@Override
	public FDSEntryItemImportPolicy getFDSEntryItemImportPolicy() {
		return FDSEntryItemImportPolicy.GROUP_PROXY;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AllSectionFDSCreationMenu.class);

	@Reference(
		target = "(frontend.data.set.name=" + CMSSiteInitializerFDSNames.ALL_SECTION + ")"
	)
	private BaseJSPSectionFragmentRenderer<ViewAllSectionDisplayContext>
		_baseJSPSectionFragmentRenderer;

}