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

package com.liferay.dynamic.data.mapping.form.evaluator.impl.internal.functions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Leonardo Barros
 */
@RunWith(MockitoJUnitRunner.class)
public class BelongsToRoleFunctionTest {

	@Before
	public void setUp() throws Exception {
		setPortalUtil();
	}

	@Test
	public void testEvaluateFalse() throws Exception {
		BelongsToRoleFunction belongsToRoleFunction = new BelongsToRoleFunction(
			_request, _userLocalService);

		mockHasRoleUser();

		Assert.assertEquals(
			false, belongsToRoleFunction.evaluate("Role0", "Role2"));
	}

	@Test
	public void testEvaluateTrue() throws Exception {
		BelongsToRoleFunction belongsToRoleFunction = new BelongsToRoleFunction(
			_request, _userLocalService);

		mockHasRoleUser();

		Assert.assertEquals(
			true, belongsToRoleFunction.evaluate("Role0", "Role1", "Role2"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument() throws Exception {
		BelongsToRoleFunction belongsToRoleFunction = new BelongsToRoleFunction(
			null, null);

		belongsToRoleFunction.evaluate();
	}

	protected void mockHasRoleUser() throws Exception {
		when(
			_userLocalService.hasRoleUser(
				_company.getCompanyId(), "Role1", _user.getUserId(), true)
		).thenReturn(
			true
		);
	}

	protected void setPortalUtil() throws Exception {
		PortalUtil portalUtil = new PortalUtil();

		Portal portal = mock(Portal.class);

		when(portal.getUser(_request)).thenReturn(_user);
		when(portal.getCompany(_request)).thenReturn(_company);

		portalUtil.setPortal(portal);
	}

	@Mock
	private Company _company;

	@Mock
	private HttpServletRequest _request;

	@Mock
	private User _user;

	@Mock
	private UserLocalService _userLocalService;

}