/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.vulcan.jaxrs.extension;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.vulcan.jackson.databind.ser.VulcanPropertyFilter;

import java.io.Serializable;

import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Gabor Komaromi
 */
public class ExtendedEntityTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testExtend() throws Exception {
		ExtendedEntity extendedEntity = ExtendedEntity.extend(
			new TestEntity(),
			HashMapBuilder.<String, Serializable>put(
				"extendedProperty1", RandomTestUtil.randomString()
			).put(
				"extendedProperty2", RandomTestUtil.randomString()
			).build(),
			null);

		JsonNode jsonNode = _serialize(extendedEntity, null, null);

		Assert.assertTrue(jsonNode.has("entityProperty1"));
		Assert.assertTrue(jsonNode.has("entityProperty2"));
		Assert.assertTrue(jsonNode.has("extendedProperty1"));
		Assert.assertTrue(jsonNode.has("extendedProperty2"));
		Assert.assertEquals(4, jsonNode.size());

		jsonNode = _serialize(
			extendedEntity,
			SetUtil.fromArray("entityProperty1", "extendedProperty1"), null);

		Assert.assertTrue(jsonNode.has("entityProperty1"));
		Assert.assertFalse(jsonNode.has("entityProperty2"));
		Assert.assertTrue(jsonNode.has("extendedProperty1"));
		Assert.assertFalse(jsonNode.has("extendedProperty2"));

		jsonNode = _serialize(
			extendedEntity, null,
			SetUtil.fromArray("entityProperty1", "extendedProperty1"));

		Assert.assertFalse(jsonNode.has("entityProperty1"));
		Assert.assertTrue(jsonNode.has("entityProperty2"));
		Assert.assertFalse(jsonNode.has("extendedProperty1"));
		Assert.assertTrue(jsonNode.has("extendedProperty2"));
	}

	private JsonNode _serialize(
			ExtendedEntity extendedEntity, Set<String> fieldNames,
			Set<String> restrictFieldNames)
		throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();

		ObjectWriter objectWriter = objectMapper.writer(
			new SimpleFilterProvider() {
				{
					addFilter(
						"Liferay.Vulcan",
						VulcanPropertyFilter.of(
							fieldNames, restrictFieldNames));
				}
			});

		return objectMapper.readTree(
			objectWriter.writeValueAsString(extendedEntity));
	}

	@JsonFilter("Liferay.Vulcan")
	private static class TestEntity {

		public String entityProperty1 = RandomTestUtil.randomString();
		public String entityProperty2 = RandomTestUtil.randomString();

	}

}