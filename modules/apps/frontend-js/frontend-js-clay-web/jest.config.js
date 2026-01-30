/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/* eslint-env node */

module.exports = {
	moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
	moduleNameMapper: {
		'@testing-library/user-event': '@testing-library/user-event-13-2-1',
	},
	resolver: `${__dirname}/clay/jest-clay-lerna-resolver.js`,
	setupFiles: [`${__dirname}/clay/setupTests.ts`, 'raf/polyfill'],
	setupFilesAfterEnv: [`${__dirname}/clay/jest-setup.ts`],
	testEnvironment: 'jsdom',
	testMatch: [
		'<rootDir>/clay/**/__tests__/**/*.[jt]s?(x)',
		'<rootDir>/clay/**/?(*.)+(spec|test).[jt]s?(x)',
	],
	testPathIgnorePatterns: [
		'browserslist-config-clay',
		'fixtures',
		'generator-clay-component',
		'lib',
		'clayui.com',
	],
	timers: 'legacy',
};
