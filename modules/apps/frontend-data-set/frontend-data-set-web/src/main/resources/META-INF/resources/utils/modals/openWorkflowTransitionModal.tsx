/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayForm, {ClayInput} from '@clayui/form';
import ClayModal from '@clayui/modal';
import {openModal} from 'frontend-js-components-web';
import React, {useRef} from 'react';

import {IItemsActions} from '../types';

const WorkflowTransitionModalComponent = ({
	action,
	closeModal,
	itemId,
	onTransitionSave,
}: {
	action: IItemsActions;
	closeModal: Function;
	itemId: string | number;
	onTransitionSave: Function;
}) => {
	const labelInputRef = useRef() as React.MutableRefObject<HTMLInputElement>;

	const {data, id} = action;

	const {title} = data ?? {};

	const fieldId = `${id}_${itemId}_transitionCommentInput`;

	return (
		<>
			<ClayModal.Header
				closeButtonAriaLabel={Liferay.Language.get('close')}
			>
				{title}
			</ClayModal.Header>

			<ClayModal.Body>
				<ClayForm.Group>
					<label htmlFor={fieldId}>
						{Liferay.Language.get('comment')}
					</label>

					<ClayInput
						autoFocus={true}
						id={fieldId}
						placeholder={Liferay.Language.get('leave-a-comment')}
						ref={labelInputRef}
						type="text"
					/>
				</ClayForm.Group>
			</ClayModal.Body>

			<ClayModal.Footer
				last={
					<ClayButton.Group spaced>
						<ClayButton
							onClick={() => {
								closeModal();
								onTransitionSave({
									comment: labelInputRef.current.value,
								});
							}}
						>
							{Liferay.Language.get('save')}
						</ClayButton>

						<ClayButton
							displayType="secondary"
							onClick={() => closeModal()}
						>
							{Liferay.Language.get('cancel')}
						</ClayButton>
					</ClayButton.Group>
				}
			/>
		</>
	);
};

export function openWorkflowTransitionModal({
	action,
	executeAsyncItemAction,
	itemId,
}: {
	action: IItemsActions;
	executeAsyncItemAction: Function;
	itemId: string | number;
}): void {
	const saveTransition = ({comment}: {comment: string}) => {
		executeAsyncItemAction({
			method: action.method,
			requestBody: JSON.stringify({
				...JSON.parse(
					action?.data?.requestBody ? action?.data?.requestBody : '{}'
				),
				comment,
			}),
			url: action.href,
		});
	};

	openModal({
		contentComponent: ({closeModal}: {closeModal: Function}) => (
			<WorkflowTransitionModalComponent
				action={action}
				closeModal={closeModal}
				itemId={itemId}
				onTransitionSave={saveTransition}
			/>
		),
	});
}
