/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.portlet.tracker.internal;

import com.liferay.portal.kernel.portlet.PortletBag;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portlet.PortletBagFactory;

import javax.portlet.Portlet;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;

/**
 * @author Raymond Augé
 */
public class BundlePortletBagFactory extends PortletBagFactory {

	public BundlePortletBagFactory(Bundle bundle, Portlet portlet) {
		_bundle = bundle;
		_portlet = portlet;
	}

	@Override
	public PortletBag create(com.liferay.portal.model.Portlet portletModel)
		throws Exception {

		if (_bundle == null) {
			throw new IllegalStateException("Bundle is null");
		}

		super.setClassLoader(_classLoader);

		ServletContext servletContext =
			(ServletContext)ProxyUtil.newProxyInstance(
				_classLoader, new Class<?>[] {ServletContext.class},
				new BundleServletContextInvocationHandler(
					_servletContext, _bundle, _classLoader));

		super.setServletContext(servletContext);

		return super.create(portletModel);
	}

	@Override
	public void setClassLoader(ClassLoader classLoader) {
		_classLoader = classLoader;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		_servletContext = servletContext;
	}

	@Override
	protected Portlet getPortletInstance(
		com.liferay.portal.model.Portlet portletModel) {

		return _portlet;
	}

	private Bundle _bundle;
	private ClassLoader _classLoader;
	private Portlet _portlet;
	private ServletContext _servletContext;

}