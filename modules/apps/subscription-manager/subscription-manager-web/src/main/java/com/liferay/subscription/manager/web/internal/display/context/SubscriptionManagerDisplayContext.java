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

package com.liferay.subscription.manager.web.internal.display.context;

import com.liferay.subscription.manager.SubscriptionManagerHandler;
import com.liferay.subscription.manager.web.internal.constants.SubscriptionManagerWebKeys;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Roberto Díaz
 */
public class SubscriptionManagerDisplayContext {

	public SubscriptionManagerDisplayContext(
		HttpServletRequest request, HttpServletResponse response) {

		_request = request;
		_response = response;
	}

	public Map<String, SubscriptionManagerHandler>
		getSubscriptionManagerHandlerMap() {

		return (Map<String, SubscriptionManagerHandler>)_request.getAttribute(
			SubscriptionManagerWebKeys.SUBSCRIPTION_MANAGER_HANDLER_MAP);
	}

	private final HttpServletRequest _request;
	private final HttpServletResponse _response;

}