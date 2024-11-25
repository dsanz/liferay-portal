/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import React, {forwardRef, useEffect, useRef, useState} from 'react';
import ReactDOM from 'react-dom';

export interface IHTMLElementBuilder<T> {
	(args: T): HTMLElement;
}

interface IClientExtensionProps<T> {
	args: T;
	htmlElementBuilder?: IHTMLElementBuilder<T>;
}

const CXWrapper = forwardRef(
	function CXWrapper<T>(props: any, ref: any) {
		return (<div ref={ref}></div>);
	});

export default function ClientExtension<T>({
	args,
	htmlElementBuilder,
}: IClientExtensionProps<T>): React.ReactElement {
	const containerRef = useRef<HTMLDivElement>(null);
	const [stringifiedArgs, setStringfiedArgs] = useState(JSON.stringify(args));

	useEffect(() => {
		const {current} = containerRef;

		if (current && htmlElementBuilder) {
			try {
				current.appendChild(htmlElementBuilder(args));
			}
			catch (error) {
				console.error(
					'The client extension implemented by the function',
					htmlElementBuilder,
					'caused an error when trying to render its HTML content.',
					'Please fix your client extension.',
					error
				);
			}
		}
		return (() => {
				if (containerRef?.current) {
					ReactDOM.unmountComponentAtNode(containerRef.current);
				}
			}
		)
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [htmlElementBuilder, stringifiedArgs]);

	return htmlElementBuilder ? (
		<CXWrapper ref={containerRef} />
	) : (
		<ClayLoadingIndicator />
	);
}
