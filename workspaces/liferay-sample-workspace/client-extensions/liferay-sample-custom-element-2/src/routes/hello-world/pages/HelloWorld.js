/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import api from '../../../common/services/liferay/api.js';
import React, {useEffect, useState} from 'react';

function HelloWorld() {
	const [name, setName] = useState("World");

	useEffect(() => {
		if (Liferay.ThemeDisplay.isSignedIn()) {
			api('o/headless-admin-user/v1.0/my-user-account')
				.then((response) => response.json())
				.then((response) => {
					if (response.givenName) {
						setName(response.givenName)
					}
				})
				.catch((error) => {
					// eslint-disable-next-line no-console
					console.log(error);
				});
		}
	});

	return (
		<div className="hello-world">
			<h1>
				Hello <span className="hello-world-name">{name}</span>
			</h1>
		</div>
	);
};

export default HelloWorld;
