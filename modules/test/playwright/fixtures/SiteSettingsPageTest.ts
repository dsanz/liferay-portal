/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// @ts-ignore

import {test} from '@playwright/test';

import {SiteSettingsPage} from '../pages/configuration-admin-web/SiteSettingsPage';

const siteSettingsPageTest = test.extend<{
	siteSettingsPage: SiteSettingsPage;
}>({
	siteSettingsPage: async ({page}, use) => {
		await use(new SiteSettingsPage(page));
	},
});

export {siteSettingsPageTest};
