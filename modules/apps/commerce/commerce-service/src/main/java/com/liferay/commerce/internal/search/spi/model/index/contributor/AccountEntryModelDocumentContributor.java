/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.commerce.internal.search.spi.model.index.contributor;

import com.liferay.account.model.AccountEntry;
import com.liferay.commerce.product.constants.CommerceChannelAccountEntryRelConstants;
import com.liferay.commerce.product.model.CommerceChannelAccountEntryRel;
import com.liferay.commerce.product.model.CommerceChannelAccountEntryRelTable;
import com.liferay.commerce.product.service.CommerceChannelAccountEntryRelLocalService;
import com.liferay.petra.sql.dsl.DSLQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.ReindexCacheThreadLocal;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.search.spi.model.index.contributor.ModelDocumentContributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Danny Situ
 */
@Component(
	property = "indexer.class.name=com.liferay.account.model.AccountEntry",
	service = ModelDocumentContributor.class
)
public class AccountEntryModelDocumentContributor
	implements ModelDocumentContributor<AccountEntry> {

	@Override
	public void contribute(Document document, AccountEntry accountEntry) {
		document.addKeyword(
			"commerceChannelIds", _getCommerceChannelIds(accountEntry));
	}

	private long[] _getCommerceChannelIds(AccountEntry accountEntry) {
		Map<Long, long[]> commerceChannelIdsMap =
			ReindexCacheThreadLocal.getGlobalReindexCache(
				() -> -1, AccountEntryModelDocumentContributor.class.getName(),
				count -> {
					Map<Long, List<Long>> commerceChannelIdListMap =
						new HashMap<>();

					for (Object[] values :
							_commerceChannelAccountEntryRelLocalService.
								<List<Object[]>>dslQuery(
									DSLQueryFactoryUtil.select(
										CommerceChannelAccountEntryRelTable.
											INSTANCE.accountEntryId,
										CommerceChannelAccountEntryRelTable.
											INSTANCE.commerceChannelId
									).from(
										CommerceChannelAccountEntryRelTable.
											INSTANCE
									).where(
										CommerceChannelAccountEntryRelTable.
											INSTANCE.type.eq(
												CommerceChannelAccountEntryRelConstants.TYPE_ELIGIBILITY)
									),
									false)) {

						List<Long> commerceChannelIds =
							commerceChannelIdListMap.computeIfAbsent(
								(Long)values[0], key -> new ArrayList<>());

						commerceChannelIds.add((Long)values[1]);
					}

					Map<Long, long[]> localCommerceChannelIdsMap =
						new HashMap<>();

					for (Map.Entry<Long, List<Long>> entry :
							commerceChannelIdListMap.entrySet()) {

						localCommerceChannelIdsMap.put(
							entry.getKey(),
							ArrayUtil.toLongArray(entry.getValue()));
					}

					return localCommerceChannelIdsMap;
				});

		if (commerceChannelIdsMap == null) {
			return ListUtil.toLongArray(
				_commerceChannelAccountEntryRelLocalService.
					getCommerceChannelAccountEntryRels(
						accountEntry.getAccountEntryId(),
						CommerceChannelAccountEntryRelConstants.
							TYPE_ELIGIBILITY,
						QueryUtil.ALL_POS, QueryUtil.ALL_POS, null),
				CommerceChannelAccountEntryRel::getCommerceChannelId);
		}

		return commerceChannelIdsMap.get(accountEntry.getAccountEntryId());
	}

	@Reference
	private CommerceChannelAccountEntryRelLocalService
		_commerceChannelAccountEntryRelLocalService;

}