/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {openCMSModal} from '../../../common/utils/openCMSModal';
import EditImageModalContent from '../../modal/EditImageModalContent';

export function isEditableImage(
	item: any,
	editableImageMIMETypes: string[] = []
) {
	return (
		Boolean(item?.embedded?.file?.link?.href) &&
		editableImageMIMETypes.includes(item?.embedded?.file?.mimeType)
	);
}

export default function editImageAction(itemData: any, loadData: () => void) {
	openCMSModal({
		contentComponent: ({closeModal}: {closeModal: () => void}) =>
			EditImageModalContent({
				closeModal,
				file: itemData.embedded.file,
				loadData,
				updateURL: itemData.actions.update.href,
			}),
		size: 'full-screen',
	});
}
