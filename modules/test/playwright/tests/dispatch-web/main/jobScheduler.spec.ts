/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {loginTest} from '../../../fixtures/loginTest';
import {ApiHelpers} from '../../../helpers/ApiHelpers';
import {liferayConfig} from '../../../liferay.config';
import getRandomString from '../../../utils/getRandomString';
import {jobSchedulerPagesTest} from './fixtures/jobSchedulerPagesTest';

export const test = mergeTests(
	apiHelpersTest,
	loginTest(),
	jobSchedulerPagesTest
);

const DISPATCH_NAMESPACE =
	'_com_liferay_dispatch_web_internal_portlet_DispatchPortlet_';

const DISPATCH_URL =
	'/group/guest/~/control_panel/manage?p_p_id=com_liferay_dispatch_web_internal_portlet_DispatchPortlet';

const PAYLOAD = "<img src=x onerror=alert('XSS98204')>";

async function invokeJSONWebService(
	apiHelpers: ApiHelpers,
	path: string,
	parameters: {[key: string]: string}
) {
	return apiHelpers.post(
		`${liferayConfig.environment.baseUrl}/api/jsonws/${path}`,
		{
			data: new URLSearchParams(parameters).toString(),
			failOnStatusCode: true,
			headers: await apiHelpers.getJSONWebServicesHeaders(),
		}
	);
}

test('can create two job triggers and can delete them', async ({
	jobSchedulerPage,
	page,
}) => {
	await jobSchedulerPage.goTo();

	await jobSchedulerPage.createNewJobSchedulerTrigger('Job Trigger 1');
	await jobSchedulerPage.createNewJobSchedulerTrigger('Job Trigger 2');

	await page.getByTestId('row').nth(0).getByRole('checkbox').check();
	await page.getByTestId('row').nth(1).getByRole('checkbox').check();

	await expect(page.getByText(/2 of \d+ Items Selected/)).toBeVisible();

	page.on('dialog', async (dialogWindow) => {
		await dialogWindow.accept();
	});

	await page.getByRole('button', {name: 'Delete'}).click();

	await expect(page.getByRole('link', {name: 'Job Trigger 1'})).toHaveCount(
		0
	);
	await expect(page.getByRole('link', {name: 'Job Trigger 2'})).toHaveCount(
		0
	);
});

test(
	'An error from a dispatch trigger setting is not executed as JavaScript in the log',
	{tag: '@LPD-98204'},
	async ({apiHelpers, page}) => {
		const plan = await apiHelpers.post(
			`${apiHelpers.baseUrl}batch-planner/v1.0/plans`,
			{
				data: {
					export: true,
					externalType: 'CSV',
					internalClassName:
						'com.liferay.headless.admin.user.dto.v1_0.UserAccount',
					name: getRandomString(),
					template: true,
				},
				failOnStatusCode: true,
			}
		);

		const userAccount =
			await apiHelpers.headlessAdminUser.getMyUserAccount();

		// The batch planner executor fails to parse the external file URL and
		// stores the parser message, which echoes the URL, as the log error

		const dispatchTriggerName = getRandomString();

		const dispatchTrigger = await invokeJSONWebService(
			apiHelpers,
			'dispatch.dispatchtrigger/add-dispatch-trigger',
			{
				dispatchTaskExecutorType: 'batch-planner',
				dispatchTaskSettingsUnicodeProperties: JSON.stringify({
					'batchPlannerPlanId': String(plan.id),
					'external-file-url': PAYLOAD,
				}),
				externalReferenceCode: '',
				name: dispatchTriggerName,
				userId: String(userAccount.id),
			}
		);

		try {
			await page.goto(DISPATCH_URL);

			await page
				.getByRole('row', {name: dispatchTriggerName})
				.getByRole('button', {name: 'Run Now'})
				.click();

			let dispatchLogs = [];

			await expect(async () => {
				dispatchLogs = await invokeJSONWebService(
					apiHelpers,
					'dispatch.dispatchlog/get-dispatch-logs',
					{
						dispatchTriggerId: dispatchTrigger.dispatchTriggerId,
						end: '1',
						start: '0',
					}
				);

				expect(dispatchLogs).toHaveLength(1);
				expect(dispatchLogs[0].error).toContain(PAYLOAD);
			}).toPass({timeout: 30 * 1000});

			const dialogs: string[] = [];

			page.on('dialog', async (dialog) => {
				dialogs.push(dialog.message());

				await dialog.dismiss();
			});

			const searchParams = new URLSearchParams({
				[`${DISPATCH_NAMESPACE}dispatchLogId`]:
					dispatchLogs[0].dispatchLogId,
				[`${DISPATCH_NAMESPACE}dispatchTriggerId`]:
					dispatchTrigger.dispatchTriggerId,
				[`${DISPATCH_NAMESPACE}mvcRenderCommandName`]:
					'/dispatch/view_dispatch_log',
			});

			await page.goto(`${DISPATCH_URL}&${searchParams}`);

			// The payload is rendered as text, not executed

			await expect(page.getByText(PAYLOAD, {exact: false})).toBeVisible();

			expect(dialogs).toHaveLength(0);
		}
		finally {
			await invokeJSONWebService(
				apiHelpers,
				'dispatch.dispatchtrigger/delete-dispatch-trigger',
				{dispatchTriggerId: dispatchTrigger.dispatchTriggerId}
			);

			await apiHelpers.delete(
				`${apiHelpers.baseUrl}batch-planner/v1.0/plans/${plan.id}`
			);
		}
	}
);
