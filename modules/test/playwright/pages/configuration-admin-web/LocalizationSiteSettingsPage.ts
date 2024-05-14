/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Locator, Page} from '@playwright/test';

import {SiteSettingsPage} from './SiteSettingsPage';

export class LocalizationSiteSettingsPage {
	readonly languageSelector: Locator;
	readonly page: Page;
	readonly saveButton: Locator;
	readonly siteSettingsPage: SiteSettingsPage;

	constructor(page: Page) {
		this.languageSelector = page.getByRole('combobox');
		this.saveButton = page.getByRole('button', {name: 'Save'});
		this.page = page;
		this.siteSettingsPage = new SiteSettingsPage(page);
	}

	async goto() {
		await this.siteSettingsPage.goToSiteSetting(
			'Localization',
			'Languages'
		);
	}

	async useDefaultLanguageOptions() {
		await this.goto();

		await this.page
			.getByRole('radio', {name: 'Use the default language options.'})
			.click();

		await this.page.getByRole('button', {name: 'Save'}).click();

		await this.page.waitForLoadState();
	}

	async setDefaultCustomLanguage(language: string) {
		await this.goto();

		await this.page
			.getByRole('radio', {name: 'Define a custom default language'})
			.click();

		await this.languageSelector.waitFor();

		await this.languageSelector.selectOption(language);

		await this.saveButton.click();

		await this.page.waitForLoadState();
	}
}
