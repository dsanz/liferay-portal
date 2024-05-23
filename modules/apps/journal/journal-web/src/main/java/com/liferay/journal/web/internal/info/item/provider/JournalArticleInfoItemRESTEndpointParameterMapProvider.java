/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.journal.web.internal.info.item.provider;

import com.liferay.info.item.InfoItemRESTEndpointParameterMap;
import com.liferay.info.item.provider.InfoItemRESTEndpointParameterMapProvider;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.util.HashMapBuilder;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(
	property = Constants.SERVICE_RANKING + ":Integer=10",
	service = InfoItemRESTEndpointParameterMapProvider.class
)
public class JournalArticleInfoItemRESTEndpointParameterMapProvider
	implements InfoItemRESTEndpointParameterMapProvider<JournalArticle> {

	@Override
	public InfoItemRESTEndpointParameterMap getInfoItemRESTEndpointParameterMap(
		JournalArticle journalArticle) {

		return new InfoItemRESTEndpointParameterMap(
			HashMapBuilder.<String, Object>put(
				"structuredContentId", journalArticle.getResourcePrimKey()
			).build());
	}

}