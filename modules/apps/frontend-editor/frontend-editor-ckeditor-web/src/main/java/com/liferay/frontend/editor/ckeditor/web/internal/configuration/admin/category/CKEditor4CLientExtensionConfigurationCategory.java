/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.editor.ckeditor.web.internal.configuration.admin.category;

import com.liferay.configuration.admin.category.ConfigurationCategory;

import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(service = ConfigurationCategory.class)
public class CKEditor4CLientExtensionConfigurationCategory
	implements ConfigurationCategory {

	@Override
	public String getBundleSymbolicName() {
		return "com.liferay.frontend.editor.ckeditor.web";
	}

	@Override
	public String getCategoryIcon() {
		return "text-editor";
	}

	@Override
	public String getCategoryKey() {
		return "rich-text-editors";
	}

	@Override
	public String getCategorySection() {
		return "platform";
	}

}