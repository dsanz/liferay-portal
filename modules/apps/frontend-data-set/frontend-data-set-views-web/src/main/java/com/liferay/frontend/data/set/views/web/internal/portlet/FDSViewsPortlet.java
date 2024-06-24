/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.views.web.internal.portlet;

import com.liferay.batch.engine.unit.BatchEngineUnitThreadLocal;
import com.liferay.client.extension.type.manager.CETManager;
import com.liferay.frontend.data.set.views.web.internal.constants.FDSViewsPortletKeys;
import com.liferay.frontend.data.set.views.web.internal.constants.FDSViewsWebKeys;
import com.liferay.frontend.data.set.views.web.internal.display.context.FDSViewsDisplayContext;
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.constants.ObjectRelationshipConstants;
import com.liferay.object.field.util.ObjectFieldUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.object.service.ObjectRelationshipLocalService;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.util.BundleUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.vulcan.util.LocalizedMapUtil;

import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Marko Cikos
 */
@Component(
	property = {
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.layout-cacheable=true",
		"javax.portlet.expiration-cache=0",
		"javax.portlet.init-param.view-template=/data_sets.jsp",
		"javax.portlet.name=" + FDSViewsPortletKeys.FDS_VIEWS,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=administrator,power-user,user",
		"javax.portlet.version=3.0"
	},
	service = Portlet.class
)
public class FDSViewsPortlet extends MVCPortlet {

	public static class CompanyScopedOpenAPIResource {

		public CompanyScopedOpenAPIResource(
			long companyId, String openAPIResourcePath) {

			_companyId = companyId;
			_openAPIResourcePath = openAPIResourcePath;
		}

		public long getCompanyId() {
			return _companyId;
		}

		public String getOpenAPIResourcePath() {
			return _openAPIResourcePath;
		}

		public boolean matches(long companyId) {
			if ((_companyId == 0) || (_companyId == companyId)) {
				return true;
			}

			return false;
		}

		private final long _companyId;
		private final String _openAPIResourcePath;

	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundle = BundleUtil.getBundle(
			bundleContext, "com.liferay.frontend.data.set.views.web");
		_serviceTrackerList = ServiceTrackerListFactory.open(
			bundleContext, null, "(openapi.resource=true)",
			new CompanyScopedRESTApplicationServiceTrackerCustomizer(
				bundleContext));
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerList.close();
	}

	@Override
	protected void doDispatch(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		renderRequest.setAttribute(
			FDSViewsWebKeys.FDS_VIEWS_DISPLAY_CONTEXT,
			new FDSViewsDisplayContext(
				_cetManager, _objectDefinitionLocalService, renderRequest,
				renderResponse, _serviceTrackerList));

		super.doDispatch(renderRequest, renderResponse);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSViewsPortlet.class);

	private Bundle _bundle;

	@Reference
	private CETManager _cetManager;

	@Reference
	private Language _language;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

	@Reference
	private ObjectRelationshipLocalService _objectRelationshipLocalService;

	private ServiceTrackerList<CompanyScopedOpenAPIResource>
		_serviceTrackerList;

	private class CompanyScopedRESTApplicationServiceTrackerCustomizer
		implements ServiceTrackerCustomizer
			<Object, CompanyScopedOpenAPIResource> {

		@Override
		public CompanyScopedOpenAPIResource addingService(
			ServiceReference<Object> serviceReference) {

			String openAPIResourcePath = (String)serviceReference.getProperty(
				"openapi.resource.path");

			if (openAPIResourcePath == null) {
				return null;
			}

			String apiVersion = (String)serviceReference.getProperty(
				"api.version");

			if (apiVersion != null) {
				openAPIResourcePath = openAPIResourcePath + "/" + apiVersion;
			}

			long companyId = GetterUtil.getLong(
				(String)serviceReference.getProperty("companyId"));

			return new CompanyScopedOpenAPIResource(
				companyId, openAPIResourcePath);
		}

		@Override
		public void modifiedService(
			ServiceReference<Object> serviceReference,
			CompanyScopedOpenAPIResource companyScopedOpenAPIResource) {
		}

		@Override
		public void removedService(
			ServiceReference<Object> serviceReference,
			CompanyScopedOpenAPIResource companyScopedOpenAPIResource) {

			_bundleContext.ungetService(serviceReference);
		}

		private CompanyScopedRESTApplicationServiceTrackerCustomizer(
			BundleContext bundleContext) {

			_bundleContext = bundleContext;
		}

		private final BundleContext _bundleContext;

	}

}