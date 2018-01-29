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

package com.liferay.subscription.manager.web.internal.comparator;

import com.liferay.subscription.manager.SubscriptionManagerHandler;

import java.util.Comparator;

/**
 * @author Roberto Díaz
 */
public class SubscriptionManagerHandlerTabOrderComparator
	implements Comparator<SubscriptionManagerHandler> {

	@Override
	public int compare(
		SubscriptionManagerHandler subscriptionManagerHandler1,
		SubscriptionManagerHandler subscriptionManagerHandler2) {

		if (subscriptionManagerHandler1.getTabPosition() <
				subscriptionManagerHandler2.getTabPosition()) {

			return -1;
		}
		else if (subscriptionManagerHandler1.getTabPosition() ==
					subscriptionManagerHandler2.getTabPosition()) {

			return 0;
		}

		else return 1;
	}

}