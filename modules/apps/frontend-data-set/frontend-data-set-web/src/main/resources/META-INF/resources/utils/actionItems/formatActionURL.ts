/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Try to replace interpolated url arguments with item properties.
 * Set _redirect and/or backURL parameters to allow navigating back
 * to the FDS component that triggered the action
 *
 * @param url URI with an optional number of interpolated parameters
 * @param item object with properties that could match interpolated parameters
 * @param target string that indicates the type of the action: link, modal, sidepanel
 *
 * @example
 * url = '/o/data-sample/{id}
 * item = {
 *   name: 'test',
 *   id: 123
 * }
 *
 * Will return '/o/data-sample/123
 *
 * See test/utils/actionItems/formatActionURL.ts for more examples
 */

import getValueFromItem from '../getValueFromItem';

import { log, error } from "console";
import * as repl from "repl";

const formatActionURL = function (
	url: string | undefined,
	item: any,
	target?: string
): string {
	if (!url) {
		return '';
	}

	let fullInterpolation = false;

	let replacedURL = url.replace(
		/(?:%7B|{)(.*?)(?:%7D|})/g,
		(match, key) => {
			const value = getValueFromItem(item, key.split('.'));

			if (match.length === url.length) {
				fullInterpolation = true;
			}

			return match.length === url.length
				? value
				: encodeURIComponent(value);
		}
	);

	if (target === 'link' && replacedURL.includes('?')) {
		const redirectionURL = window.location.href;

		if (fullInterpolation) {
			replacedURL = encodeURI(replacedURL);
		}

		const hashIndex = replacedURL.indexOf('#');

		const searchParams = new URLSearchParams(
			hashIndex === -1 ? replacedURL.slice(replacedURL.indexOf('?')) :
			replacedURL.slice(replacedURL.indexOf('?'), hashIndex)
		);

		const backURL = 'backURL';
		const redirect = 'redirect';
		const backURLRegexp = new RegExp(backURL);
		const redirectRegexp = new RegExp(redirect);

		const p_p_id = searchParams.get('p_p_id');

		if (redirectRegexp.test(url) || backURLRegexp.test(url)) {
			for (const key of searchParams.keys()) {
				if (redirectRegexp.test(key) || backURLRegexp.test(key)) {
					searchParams.set(key, redirectionURL);
				}
			}
		}
		else if (p_p_id) {
			const backURLParam = `_${p_p_id}_${backURL}`;
			const redirectParam = `_${p_p_id}_${redirect}`;

			searchParams.set(redirectParam, redirectionURL);
			searchParams.set(backURLParam, redirectionURL);
		}

		log(searchParams.toString());

		/*const updatedURL = decodeURIComponent(
			`${replacedURL.slice(
				0,
				replacedURL.indexOf('?')
			)}?${searchParams.toString()}`
		); */

		const updatedURL =
			decodeURIComponent(`${replacedURL.slice(
				0,
				replacedURL.indexOf('?')
			)}`) + '?' +
			decodeURIComponent(searchParams.toString())
			+
			(hashIndex !== -1 ?
				(fullInterpolation ? decodeURIComponent(replacedURL.slice(hashIndex)) : replacedURL.slice(hashIndex)) : '');

		log(updatedURL);

		return updatedURL;
	}

	return replacedURL;
};

export default formatActionURL;
