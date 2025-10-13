/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.site.cms.site.initializer.internal.frontend.data.set;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.petra.string.StringPool;
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
	service = SystemFDSEntry.class
)
public class AllSectionSystemFDSEntry implements SystemFDSEntry {

	@Override
	public String getAdditionalAPIURLParameters(
		HttpServletRequest httpServletRequest) {

		try {
			ViewAllSectionDisplayContext viewAllSectionDisplayContext =
				_baseJSPSectionFragmentRenderer.getDisplayContext(
					httpServletRequest);

			String apiURL = viewAllSectionDisplayContext.getAPIURL();

			return apiURL.substring(apiURL.lastIndexOf('?') + 1);
		}
		catch (PortalException portalException) {
			_log.error(
				"Unable to get additional API URL parameters", portalException);
		}

		return StringPool.BLANK;
	}

	@Override
	public int getDefaultItemsPerPage() {
		return 20;
	}

	@Override
	public String getDescription() {
		return "CMS All Section";
	}

	@Override
	public String getName() {
		return CMSSiteInitializerFDSNames.ALL_SECTION;
	}

	@Override
	public String getPropsTransformer() {
		return "{AssetsFilesDropFDSPropsTransformer} from " +
			"site-cms-site-initializer";
	}

	@Override
	public String getRESTApplication() {
		return "/search/v1.0";
	}

	@Override
	public String getRESTEndpoint() {
		return "/v1.0/search";
	}

	@Override
	public String getRESTSchema() {
		return "SearchResult";
	}

	@Override
	public String getSymbol() {
		return "cog";
	}

	@Override
	public String getTitle() {
		return "All Section";
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AllSectionSystemFDSEntry.class);

	@Reference(
		target = "(frontend.data.set.name=" + CMSSiteInitializerFDSNames.ALL_SECTION + ")"
	)
	private BaseJSPSectionFragmentRenderer<ViewAllSectionDisplayContext>
		_baseJSPSectionFragmentRenderer;

}