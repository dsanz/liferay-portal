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

package com.liferay.subscription.manager.web.internal.portlet.action;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.subscription.manager.SubscriptionManagerHandler;
import com.liferay.subscription.manager.web.internal.constants.SubscriptionManagerWebKeys;
import com.liferay.subscription.manager.web.internal.registry.SubscriptionManagerHandlerRegistry;
import com.liferay.subscription.manager.web.internal.util.SubscriptionManagerPortletKeys;

import java.util.Map;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Roberto Díaz
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + SubscriptionManagerPortletKeys.SUBSCRIPTION_MANAGER,
		"mvc.command.name=/", "mvc.command.name=/subscription_manager/view"
	},
	service = MVCRenderCommand.class
)
public class SubscriptionManagerViewMVCRenderCommand
	implements MVCRenderCommand {

	@Override
	public String render(
		RenderRequest renderRequest, RenderResponse renderResponse) {

		Map<String, SubscriptionManagerHandler> subscriptionManagerHandlerMap =
			_subscriptionManagerHandlerRegistry.
				getSubscriptionManagerHandlerMap();

		renderRequest.setAttribute(
			SubscriptionManagerWebKeys.SUBSCRIPTION_MANAGER_HANDLER_MAP,
			subscriptionManagerHandlerMap);

		return "/subscription_manager/view.jsp";
	}

	@Reference(unbind = "-")
	public void setSubscriptionManagerHandlerRegistry(
		SubscriptionManagerHandlerRegistry subscriptionManagerHandlerRegistry) {

		_subscriptionManagerHandlerRegistry =
			subscriptionManagerHandlerRegistry;
	}

	private SubscriptionManagerHandlerRegistry
		_subscriptionManagerHandlerRegistry;

}