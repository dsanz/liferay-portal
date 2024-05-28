/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.web.internal.info.item.provider;

import com.liferay.info.item.InfoItemRESTEndpointParameterMap;
import com.liferay.info.item.provider.InfoItemRESTEndpointParameterMapProvider;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.HashMapBuilder;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = Constants.SERVICE_RANKING + ":Integer=10",
	service = InfoItemRESTEndpointParameterMapProvider.class
)
public class ObjectEntryInfoItemRESTEndpointParameterMapProvider
	implements InfoItemRESTEndpointParameterMapProvider<ObjectEntry> {

	@Override
	public InfoItemRESTEndpointParameterMap getInfoItemRESTEndpointParameterMap(
		ObjectEntry objectEntry) {

		try {
			ObjectDefinition objectDefinition =
				_objectDefinitionService.getObjectDefinition(
					objectEntry.getObjectDefinitionId());

			return new InfoItemRESTEndpointParameterMap(
				HashMapBuilder.<String, Object>put(
					objectDefinition.getShortName() + "Id",
					objectEntry.getObjectEntryId()
				).build());
		}
		catch (PortalException e) {
		}

		return null;
	}

	@Reference
	private ObjectDefinitionService _objectDefinitionService;

}