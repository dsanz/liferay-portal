/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.editor.ckeditor.web.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Daniel Sanz
 */
@ExtendedObjectClassDefinition(
	category = "rich-text-editors", factoryInstanceLabelAttribute = "editor.Id",
	scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	description = "ckeditor4-client-extension-configuration-help",
	factory = true,
	id = "com.liferay.frontend.editor.ckeditor.web.internal.configuration.CKEditor4ClientExtensionConfiguration",
	localization = "content/Language",
	name = "ckeditor4-client-extension-configuration-name"
)
public interface CKEditor4ClientExtensionConfiguration {

	@Meta.AD(
		description = "editor-id-help", id = "editor.Id", name = "editor-id"
	)
	public String editorId();

	@Meta.AD(
		description = "client-extension-erc-help", id = "client.extension.erc",
		name = "client-extension-erc"
	)
	public String clientExtensionERC();

}