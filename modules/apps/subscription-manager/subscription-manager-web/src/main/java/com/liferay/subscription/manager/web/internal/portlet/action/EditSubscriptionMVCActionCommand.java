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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.SubscriptionLocalService;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.subscription.manager.web.internal.registry.SubscriptionManagerHandlerRegistry;
import com.liferay.subscription.manager.web.internal.util.SubscriptionManagerPortletKeys;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Roberto Díaz
 */
@Component(
	property = {
		"javax.portlet.name=" + SubscriptionManagerPortletKeys.SUBSCRIPTION_MANAGER,
		"mvc.command.name=/subscription_manager/edit_subscription"
	},
	service = MVCActionCommand.class
)
public class EditSubscriptionMVCActionCommand extends BaseMVCActionCommand {

	@Reference(unbind = "-")
	public void setSubscriptionManagerHandlerRegistry(
		SubscriptionManagerHandlerRegistry subscriptionManagerHandlerRegistry) {

		_subscriptionManagerHandlerRegistry =
			subscriptionManagerHandlerRegistry;
	}

	public void subscribeUsers(ActionRequest actionRequest)
		throws PortalException {

		String className = ParamUtil.getString(actionRequest, "className");
		long classPK = ParamUtil.getLong(actionRequest, "classPK");
		long groupId = ParamUtil.getLong(actionRequest, "groupId");
		long[] userIds = ParamUtil.getLongValues(actionRequest, "userIds");

		for (long userId : userIds) {
			_subscriptionLocalService.addSubscription(
				userId, groupId, className, classPK);
		}
	}

	public void unsubscribeUsers(ActionRequest actionRequest)
		throws PortalException {

		String className = ParamUtil.getString(actionRequest, "className");
		long classPK = ParamUtil.getLong(actionRequest, "classPK");
		long groupId = ParamUtil.getLong(actionRequest, "groupId");
		long[] userIds = ParamUtil.getLongValues(actionRequest, "userIds");

		Group group = _groupLocalService.getGroup(groupId);

		for (long userId : userIds) {
			if (!_subscriptionLocalService.isSubscribed(
					group.getCompanyId(), userId, className, classPK)) {

				continue;
			}

			_subscriptionLocalService.deleteSubscription(
				userId, className, classPK);
		}
	}

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		try {
			if (cmd.equals(Constants.SUBSCRIBE)) {
				subscribeUsers(actionRequest);
			}
			else if (cmd.equals(Constants.UNSUBSCRIBE)) {
				unsubscribeUsers(actionRequest);
			}
		}
		catch (PortalException pe) {
			SessionErrors.add(actionRequest, pe.getClass());

			actionResponse.setRenderParameter(
				"mvcPath", "/subscription_manager/error.jsp");
		}
	}

	@Reference(unbind = "-")
	protected void setGroupLocalService(GroupLocalService groupLocalService) {
		_groupLocalService = groupLocalService;
	}

	@Reference(unbind = "-")
	protected void setSubscriptionLocalService(
		SubscriptionLocalService subscriptionLocalService) {

		_subscriptionLocalService = subscriptionLocalService;
	}

	private GroupLocalService _groupLocalService;
	private SubscriptionLocalService _subscriptionLocalService;
	private SubscriptionManagerHandlerRegistry
		_subscriptionManagerHandlerRegistry;

}