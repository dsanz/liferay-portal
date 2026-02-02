/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getObjectValueFromPath} from 'frontend-js-web';

import {getLocalizedValue} from '../getLocalizedValue';
import {
	EItemActionsType,
	IItemActionsData,
	IItemActionsDataFilter,
	IItemsActions,
} from '../types';
import {ACTION_ITEM_TARGETS} from './constants';

const hasPermission = (action: IItemsActions, itemData: any): boolean => {
	if (!action?.data?.permissionKey) {
		return true;
	}

	if (!itemData?.actions) {
		return false;
	}

	const permissionKey = action.data?.permissionKey?.toLowerCase();

	return Object.keys(itemData?.actions).some(
		(itemAction) => itemAction.toLowerCase() === permissionKey
	);
};

const matchesVisibilityFilters = (
	action: IItemsActions,
	itemData: any
): boolean => {
	if (!action?.data?.visibilityFilters) {
		return true;
	}

	const visibilityFilters: IItemActionsDataFilter =
		action?.data?.visibilityFilters;

	return Object.keys(visibilityFilters).every(
		(key: string) =>
			getLocalizedValue(itemData, key)?.value === visibilityFilters[key]
	);
};

const isDisabled = (
	action: IItemsActions,
	infoPanelOpen: boolean,
	itemData: any,
	selectedItem: boolean
): boolean => {
	if (action?.isDisabled) {
		return action.isDisabled(itemData);
	}

	if (
		infoPanelOpen &&
		action.target === ACTION_ITEM_TARGETS.INFO_PANEL &&
		selectedItem
	) {
		return true;
	}

	return false;
};

const isVisible = (
	action: IItemsActions,
	itemData: any,
	selectable: boolean = false
): boolean => {
	if (
		!hasPermission(action, itemData) ||
		!matchesVisibilityFilters(action, itemData) ||
		(action.target === ACTION_ITEM_TARGETS.INFO_PANEL && !selectable)
	) {
		return false;
	}

	if (action?.isVisible) {
		return action.isVisible(itemData);
	}

	return true;
};

type TInterpolateData = IItemsActions | IItemActionsData;

function interpolateActionProperty<T extends TInterpolateData>({
	actionData,
	itemData,
	key,
	object,
}: {
	actionData?: IItemActionsData;
	itemData: any;
	key: keyof T;
	object: T;
}) {
	if (typeof object[key] === 'string') {
		(object[key] as unknown as string) = (
			object[key] as unknown as string
		).replace(/\{([^}]+)}/g, (_match: string, group: string) => {
			if (actionData?.interpolationType === 'array') {
				if (actionData?.interpolationSource?.length === group.length) {
					return itemData;
				}
			}

			if (actionData?.interpolationType === 'item') {
				if (actionData?.interpolationSource && group.includes(actionData?.interpolationSource)) {
					return _match;
				}
			}

			return getObjectValueFromPath({
				object: itemData,
				path: actionData?.interpolationSource
					? group.substring(actionData.interpolationSource.length + 1)
					: group,
			});
		});
	}
}

const transformAction = ({
	action,
	infoPanelOpen,
	itemData,
	selectedItem,
}: {
	action: IItemsActions;
	infoPanelOpen: boolean;
	itemData: any;
	selectedItem: boolean;
}): IItemsActions => {
	action.disabled = isDisabled(action, infoPanelOpen, itemData, selectedItem);

	if (!action?.data?.permissionKey) {
		return action;
	}

	const permissionKey = action?.data?.permissionKey?.toLowerCase();

	const matchedPermissionKeys = Object.keys(itemData?.actions).filter(
		(itemAction) => itemAction.toLowerCase() === permissionKey
	);

	if (!matchedPermissionKeys.length) {
		return action;
	}

	if (action?.target === 'headless') {
		action = {
			...action,
			...itemData?.actions[matchedPermissionKeys[0]],
		};
	}

	return action;
};

const expandActions = (
	actions: Array<IItemsActions>,
	itemData: any
): Array<IItemsActions> => {
	if (!actions) {
		return [];
	}

	return actions.flatMap((action) => {
		const newAction: IItemsActions = {...action};

		const actionData: IItemActionsData | undefined = newAction.data;

		Object.keys(newAction).forEach((key) => {
			const typedKey = key as keyof IItemsActions;

			if (
				!actionData?.interpolationSource ||
				!typedKey.includes(actionData?.interpolationSource)
			) {
				interpolateActionProperty({
					actionData,
					itemData,
					key: typedKey,
					object: newAction,
				});
			}
		});

		if (actionData) {
			Object.keys(actionData).forEach((key) => {
				const typedKey = key as keyof IItemActionsData;

				if (
					!actionData.interpolationSource ||
					!typedKey.includes(actionData.interpolationSource)
				) {
					interpolateActionProperty({
						actionData,
						itemData,
						key: typedKey,
						object: actionData,
					});
				}
			});

			if (
				actionData.interpolationType === 'array' &&
				actionData.interpolationSource
			) {
				const sourcePath = action?.data?.interpolationSource;

				if (!sourcePath) {
					return [];
				}

				const arrayItemData = getObjectValueFromPath({
					object: itemData,
					path: sourcePath,
				});

				if (!Array.isArray(arrayItemData)) {
					return [];
				}

				return arrayItemData.map((arrayItem: any) => {
					const expandedAction: IItemsActions = {...newAction};

					Object.keys(newAction).forEach((key) => {
						const typedKey = key as keyof IItemsActions;

						interpolateActionProperty({
							actionData,
							itemData: arrayItem,
							key: typedKey,
							object: expandedAction,
						});
					});

					if (expandedAction.data) {
						const actionData = expandedAction.data;

						if (actionData) {
							Object.keys(actionData).forEach((key) => {
								const typedKey = key as keyof IItemActionsData;

								interpolateActionProperty({
									actionData,
									itemData: arrayItem,
									key: typedKey,
									object: actionData,
								});
							});
						}
					}

					return expandedAction;
				});
			}
		}
		if (newAction.items) {
			return {
				...newAction,
				items: expandActions(newAction.items, itemData),
			};
		}

		return newAction;
	});
};

const transformItemActions = ({
	actions,
	infoPanelOpen = false,
	itemData,
	selectable,
	selectedItemsKey,
	selectedItemsValue,
}: {
	actions: Array<IItemsActions>;
	infoPanelOpen?: boolean;
	itemData: any;
	selectable?: boolean;
	selectedItemsKey: string;
	selectedItemsValue?: Array<any>;
}): Array<IItemsActions> => {
	const selectedItem =
		selectedItemsValue?.length === 1 &&
		!!selectedItemsValue?.includes(
			getObjectValueFromPath({object: itemData, path: selectedItemsKey})
		);

	return actions
		? actions
				.filter((action: IItemsActions) =>
					isVisible(action, itemData, selectable)
				)
				.map((action: IItemsActions) => {
					const transformedAction = transformAction({
						action,
						infoPanelOpen,
						itemData,
						selectedItem,
					});

					if (
						(action.type === EItemActionsType.GROUP ||
							action.type === EItemActionsType.CONTEXTUAL) &&
						action.items
					) {
						return {
							...transformedAction,
							items: transformItemActions({
								actions: action.items,
								infoPanelOpen,
								itemData,
								selectable,
								selectedItemsKey,
								selectedItemsValue,
							}),
						};
					}

					return transformedAction;
				})
		: [];
};

const filterItemActions = ({
	actions,
	infoPanelOpen = false,
	itemData,
	selectable,
	selectedItemsKey,
	selectedItemsValue,
}: {
	actions: Array<IItemsActions>;
	infoPanelOpen?: boolean;
	itemData: any;
	selectable?: boolean;
	selectedItemsKey: string;
	selectedItemsValue?: Array<any>;
}): Array<IItemsActions> => {
	const expandedActions = expandActions(actions, itemData);

	return transformItemActions({
		actions: expandedActions,
		infoPanelOpen,
		itemData,
		selectable,
		selectedItemsKey,
		selectedItemsValue,
	});
};

export default filterItemActions;
