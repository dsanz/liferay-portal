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

package com.liferay.portal.workflow.kaleo.runtime.integration.impl.internal.util;

import static com.liferay.portal.kernel.test.util.RandomTestUtil.randomLong;
import static com.liferay.portal.kernel.util.StringUtil.randomString;
import static com.liferay.portal.workflow.kaleo.runtime.integration.impl.internal.util.KaleoRuntimeTestUtil.assertWorkflowTaskAssignee;
import static com.liferay.portal.workflow.kaleo.runtime.integration.impl.internal.util.KaleoRuntimeTestUtil.mockKaleoTaskAssignmentInstance;
import static com.liferay.portal.workflow.kaleo.runtime.integration.impl.internal.util.KaleoRuntimeTestUtil.mockKaleoTaskInstanceToken;

import com.liferay.portal.kernel.workflow.WorkflowTaskAssignee;
import com.liferay.portal.workflow.kaleo.model.KaleoTaskAssignmentInstance;
import com.liferay.portal.workflow.kaleo.model.KaleoTaskInstanceToken;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marcellus Tavares
 */
public class FirstWorkflowTaskAssigneeSupplierTest {

	@Test
	public void testGetWhenFirstKaleoTaskAssignmentInstanceIsNotNull() {
		String expectedAssigneeClassName = randomString();

		long expectedAssigneeClassPK = randomLong();

		KaleoTaskAssignmentInstance firstKaleoTaskAssignmentInstance =
			mockKaleoTaskAssignmentInstance(
				expectedAssigneeClassName, expectedAssigneeClassPK);

		KaleoTaskInstanceToken kaleoTaskInstanceToken =
			mockKaleoTaskInstanceToken(firstKaleoTaskAssignmentInstance);

		FirstWorkflowTaskAssigneeSupplier firstWorkflowTaskAssigneeSupplier =
			new FirstWorkflowTaskAssigneeSupplier(kaleoTaskInstanceToken);

		Optional<WorkflowTaskAssignee> firstWorkflowTaskAssigneeOptional =
			firstWorkflowTaskAssigneeSupplier.get();

		Assert.assertTrue(firstWorkflowTaskAssigneeOptional.isPresent());

		assertWorkflowTaskAssignee(
			expectedAssigneeClassName, expectedAssigneeClassPK,
			firstWorkflowTaskAssigneeOptional.get());
	}

	@Test
	public void testGetWhenFirstKaleoTaskAssignmentInstanceIsNull() {
		KaleoTaskInstanceToken kaleoTaskInstanceToken =
			mockKaleoTaskInstanceToken();

		FirstWorkflowTaskAssigneeSupplier firstWorkflowTaskAssigneeSupplier =
			new FirstWorkflowTaskAssigneeSupplier(kaleoTaskInstanceToken);

		Optional<WorkflowTaskAssignee> firstWorkflowTaskAssigneeOptional =
			firstWorkflowTaskAssigneeSupplier.get();

		Assert.assertFalse(firstWorkflowTaskAssigneeOptional.isPresent());
	}

}