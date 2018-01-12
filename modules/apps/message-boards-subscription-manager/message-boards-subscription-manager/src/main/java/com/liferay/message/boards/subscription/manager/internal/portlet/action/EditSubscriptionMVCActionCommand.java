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

package com.liferay.message.boards.subscription.manager.internal.portlet.action;

import com.liferay.message.boards.kernel.model.MBCategory;
import com.liferay.message.boards.kernel.service.MBCategoryLocalService;
import com.liferay.message.boards.subscription.manager.internal.util.MBSubscriptionManagerPortletKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.SubscriptionLocalService;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Roberto Díaz
 */
@Component(
	property = {
		"javax.portlet.name=" + MBSubscriptionManagerPortletKeys.MESSAGE_BOARDS_SUBSCRIPTION_MANAGER,
		"mvc.command.name=/message_boards_subscription_manager/edit_subscription"
	},
	service = MVCActionCommand.class
)
public class EditSubscriptionMVCActionCommand extends BaseMVCActionCommand {

	public void subscribeUsers(ActionRequest actionRequest)
		throws PortalException {

		long mbCategoryId = ParamUtil.getLong(actionRequest, "mbCategoryId");

		MBCategory mbCategory = _mbCategoryLocalService.getMBCategory(
			mbCategoryId);

		long[] userIds = ParamUtil.getLongValues(actionRequest, "userIds");

		for (long userId : userIds) {
			_subscriptionLocalService.addSubscription(
				userId, mbCategory.getGroupId(), MBCategory.class.getName(),
				mbCategory.getCategoryId());
		}
	}

	public void unsubscribeUsers(ActionRequest actionRequest)
		throws PortalException {

		long mbCategoryId = ParamUtil.getLong(actionRequest, "mbCategoryId");

		MBCategory mbCategory = _mbCategoryLocalService.getMBCategory(
			mbCategoryId);

		long[] userIds = ParamUtil.getLongValues(actionRequest, "userIds");

		for (long userId : userIds) {
			if (!_subscriptionLocalService.isSubscribed(
					mbCategory.getCompanyId(), userId,
					MBCategory.class.getName(), mbCategoryId)) {

				continue;
			}

			_subscriptionLocalService.deleteSubscription(
				userId, MBCategory.class.getName(), mbCategoryId);
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
				"mvcPath", "/message_boards_subscription_manager/error.jsp");
		}
	}

	@Reference(unbind = "-")
	protected void setMBCategoryLocalService(
		MBCategoryLocalService mbCategoryLocalService) {

		_mbCategoryLocalService = mbCategoryLocalService;
	}

	@Reference(unbind = "-")
	protected void setMBCategoryService(
		SubscriptionLocalService subscriptionLocalService) {

		_subscriptionLocalService = subscriptionLocalService;
	}

	private MBCategoryLocalService _mbCategoryLocalService;
	private SubscriptionLocalService _subscriptionLocalService;

}