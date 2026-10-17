/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.adaptive.media.image.internal.util;

import com.liferay.adaptive.media.image.configuration.AMImageConfigurationEntry;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.FeatureFlag;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Daniel Sanz
 */
@FeatureFlag("LPD-94784")
public class AMImageAdaptationUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testIsAdaptationDelegatedWhenAdaptationDelegationIsNotSet() {
		Assert.assertFalse(
			AMImageAdaptationUtil.isAdaptationDelegated(
				_createAMImageConfigurationEntry(null), _COMPANY_ID));
	}

	@Test
	public void testIsAdaptationDelegatedWhenAdaptationIsDelegated() {
		Assert.assertTrue(
			AMImageAdaptationUtil.isAdaptationDelegated(
				_createAMImageConfigurationEntry("true"), _COMPANY_ID));
	}

	@Test
	public void testIsAdaptationDelegatedWhenAdaptationIsNotDelegated() {
		Assert.assertFalse(
			AMImageAdaptationUtil.isAdaptationDelegated(
				_createAMImageConfigurationEntry("false"), _COMPANY_ID));
	}

	@FeatureFlag(enable = false, value = "LPD-94784")
	@Test
	public void testIsAdaptationDelegatedWhenTheFeatureFlagIsDisabled() {
		Assert.assertFalse(
			AMImageAdaptationUtil.isAdaptationDelegated(
				_createAMImageConfigurationEntry("true"), _COMPANY_ID));
	}

	@Test
	public void testIsAdaptationDelegatedWhenThereIsNoAMImageConfigurationEntry() {
		Assert.assertFalse(
			AMImageAdaptationUtil.isAdaptationDelegated(null, _COMPANY_ID));
	}

	private AMImageConfigurationEntry _createAMImageConfigurationEntry(
		String delegateImageAdaptation) {

		Map<String, String> properties = new HashMap<>();

		if (delegateImageAdaptation != null) {
			properties.put(
				"delegate-image-adaptation", delegateImageAdaptation);
		}

		AMImageConfigurationEntry amImageConfigurationEntry = Mockito.mock(
			AMImageConfigurationEntry.class);

		Mockito.when(
			amImageConfigurationEntry.getProperties()
		).thenReturn(
			properties
		);

		return amImageConfigurationEntry;
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

}