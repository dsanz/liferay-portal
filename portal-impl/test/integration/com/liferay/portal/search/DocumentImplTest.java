/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.search;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.search.BaseIndexerPostProcessor;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.SortFactoryUtil;
import com.liferay.portal.kernel.test.ExecutionTestListeners;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.test.EnvironmentExecutionTestListener;
import com.liferay.portal.test.LiferayIntegrationJUnitTestRunner;
import com.liferay.portal.test.Sync;
import com.liferay.portal.test.SynchronousDestinationExecutionTestListener;
import com.liferay.portal.test.TransactionalExecutionTestListener;
import com.liferay.portlet.usersadmin.util.UserIndexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Sanz
 */
@ExecutionTestListeners(
	listeners = {
		EnvironmentExecutionTestListener.class,
		SynchronousDestinationExecutionTestListener.class,
		TransactionalExecutionTestListener.class
	})
@RunWith(LiferayIntegrationJUnitTestRunner.class)
@Sync
public class DocumentImplTest {

	public static final String MULTI_DOUBLE = "md";

	public static final String MULTI_FLOAT = "mf";

	public static final String MULTI_INT = "mi";

	public static final String MULTI_LONG = "ml";

	public static final String SINGLE_DOUBLE = "sd";

	public static final String SINGLE_FLOAT = "sf";

	public static final String SINGLE_INT = "si";

	public static final String SINGLE_LONG = "sl";

	@Before
	public void setUp() throws Exception {
		_indexer = IndexerRegistryUtil.getIndexer(UserIndexer.class);

		_indexer.registerIndexerPostProcessor(
			new ExtendedUserIndexerPostProcessor());

		populateUsersNumbers();

		for (String screenName : _userSingleDouble.keySet()) {
			String firstName = screenName.replaceFirst(
				"User", StringPool.BLANK);

			ServiceTestUtil.addUser(
				screenName, false, firstName, "Smith", null);
		}
	}

	@Test
	public void testFirstNameSearchResultsCount() throws Exception {
		checkNumberOfSearchResults(buildSearchContext("first"), 1);

		checkNumberOfSearchResults(buildSearchContext("second"), 1);

		checkNumberOfSearchResults(buildSearchContext("third"), 1);

		checkNumberOfSearchResults(buildSearchContext("fourth"), 1);

		checkNumberOfSearchResults(buildSearchContext("fifth"), 1);

		checkNumberOfSearchResults(buildSearchContext("sixth"), 1);
	}

	@Test
	public void testFirstNamesSearchResultsCount() throws Exception {
		checkNumberOfSearchResults(
			buildSearchContext("sixth second first fourth fifth third"), 6);
	}

	@Test
	public void testFirstNamesSearchSortedByMultiDouble() throws Exception {
		String[] expected =
			new String[] {"firstUser", "thirdUser", "fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"third sixth fifth first", expected, MULTI_DOUBLE,
			Sort.DOUBLE_TYPE);

		checkSortedSearchResultsOrder(
			"sixth fifth first third", expected, MULTI_DOUBLE,
			Sort.DOUBLE_TYPE);

		checkSortedSearchResultsOrder(
			"fifth first third sixth", expected, MULTI_DOUBLE,
			Sort.DOUBLE_TYPE);

		checkSortedSearchResultsOrder(
			"first third sixth fifth", expected, MULTI_DOUBLE,
			Sort.DOUBLE_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedByMultiFloat() throws Exception {
		String[] expected =
			new String[] {"firstUser", "secondUser", "fourthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"sixth second first fourth", expected, MULTI_FLOAT,
			Sort.FLOAT_TYPE);

		checkSortedSearchResultsOrder(
			"second first fourth sixth", expected, MULTI_FLOAT,
			Sort.FLOAT_TYPE);

		checkSortedSearchResultsOrder(
			"first fourth sixth second", expected, MULTI_FLOAT,
			Sort.FLOAT_TYPE);

		checkSortedSearchResultsOrder(
			"fourth sixth second first", expected, MULTI_FLOAT,
			Sort.FLOAT_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedByMultiInteger() throws Exception {
		String[] expected =
			new String[] {"fourthUser", "fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"fourth fifth sixth", expected, MULTI_INT, Sort.INT_TYPE);

		checkSortedSearchResultsOrder(
			"fifth sixth fourth", expected, MULTI_INT, Sort.INT_TYPE);

		checkSortedSearchResultsOrder(
			"sixth fourth fifth", expected, MULTI_INT, Sort.INT_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedByMultiLong() throws Exception {
		String[] expected =
			new String[] {"firstUser", "secondUser", "thirdUser", "fourthUser",
				"fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"sixth second first fourth fifth third", expected, MULTI_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"second first fourth fifth third sixth", expected, MULTI_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"first fourth fifth third sixth second", expected, MULTI_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"fourth fifth third sixth second first", expected, MULTI_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"fifth third sixth second first fourth", expected, MULTI_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"third sixth second first fourth fifth", expected, MULTI_LONG,
			Sort.LONG_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedBySingleDouble() throws Exception {
		String[] expected =
			new String[] {"firstUser", "thirdUser", "fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"third sixth fifth first", expected, SINGLE_DOUBLE,
			Sort.DOUBLE_TYPE);

		checkSortedSearchResultsOrder(
			"sixth fifth first third", expected, SINGLE_DOUBLE,
			Sort.DOUBLE_TYPE);

		checkSortedSearchResultsOrder(
			"fifth first third sixth", expected, SINGLE_DOUBLE,
			Sort.DOUBLE_TYPE);

		checkSortedSearchResultsOrder(
			"first third sixth fifth", expected, SINGLE_DOUBLE,
			Sort.DOUBLE_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedBySingleFloat() throws Exception {
		String[] expected =
			new String[] {"firstUser", "secondUser", "fourthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"sixth second first fourth", expected, SINGLE_FLOAT,
			Sort.FLOAT_TYPE);

		checkSortedSearchResultsOrder(
			"second first fourth sixth", expected, SINGLE_FLOAT,
			Sort.FLOAT_TYPE);

		checkSortedSearchResultsOrder(
			"first fourth sixth second", expected, SINGLE_FLOAT,
			Sort.FLOAT_TYPE);

		checkSortedSearchResultsOrder(
			"fourth sixth second first", expected, SINGLE_FLOAT,
			Sort.FLOAT_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedBySingleInteger() throws Exception {
		String[] expected =
			new String[] {"fourthUser", "fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"fourth fifth sixth", expected, SINGLE_INT, Sort.INT_TYPE);

		checkSortedSearchResultsOrder(
			"fifth sixth fourth", expected, SINGLE_INT, Sort.INT_TYPE);

		checkSortedSearchResultsOrder(
			"sixth fourth fifth", expected, SINGLE_INT, Sort.INT_TYPE);
	}

	@Test
	public void testFirstNamesSearchSortedBySingleLong() throws Exception {
		String[] expected =
			new String[] {"firstUser", "secondUser", "thirdUser", "fourthUser",
				"fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"sixth second first fourth fifth third", expected, SINGLE_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"second first fourth fifth third sixth", expected, SINGLE_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"first fourth fifth third sixth second", expected, SINGLE_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"fourth fifth third sixth second first", expected, SINGLE_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"fifth third sixth second first fourth", expected, SINGLE_LONG,
			Sort.LONG_TYPE);

		checkSortedSearchResultsOrder(
			"third sixth second first fourth fifth", expected, SINGLE_LONG,
			Sort.LONG_TYPE);
	}

	@Test
	public void testLastNameSearchResultsCount() throws Exception {
		checkNumberOfSearchResults(buildSearchContext("Smith"), 6);
	}

	@Test
	public void testLastNameSearchSortedByMultiDouble() throws Exception {
		String[] expected =
			new String[] {"firstUser", "thirdUser", "fifthUser", "secondUser",
				"fourthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"Smith", expected, MULTI_DOUBLE, Sort.DOUBLE_TYPE);
	}

	@Test
	public void testLastNameSearchSortedByMultiInteger() throws Exception {
		String[] expected =
			new String[] {"fourthUser", "fifthUser", "sixthUser", "firstUser",
				"secondUser", "thirdUser"};

		checkSortedSearchResultsOrder(
			"Smith", expected, MULTI_INT, Sort.INT_TYPE);
	}

	@Test
	public void testLastNameSearchSortedByMultiLong() throws Exception {
		String[] expected =
			new String[] {"firstUser", "secondUser", "thirdUser", "fourthUser",
				"fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"Smith", expected, MULTI_LONG, Sort.LONG_TYPE);
	}

	@Test
	public void testLastNameSearchSortedBySingleDouble() throws Exception {
		String[] expected =
			new String[] {"firstUser", "thirdUser", "fifthUser", "secondUser",
				"fourthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"Smith", expected, SINGLE_DOUBLE, Sort.DOUBLE_TYPE);
	}

	@Test
	public void testLastNameSearchSortedBySingleInteger() throws Exception {
		String[] expected =
			new String[] {"fourthUser", "fifthUser", "sixthUser", "firstUser",
						"secondUser", "thirdUser"};

		checkSortedSearchResultsOrder(
			"Smith", expected, SINGLE_INT, Sort.INT_TYPE);
	}

	@Test
	public void testLastNameSearchSortedBySingleLong() throws Exception {
		String[] expected =
			new String[] {"firstUser", "secondUser", "thirdUser", "fourthUser",
				"fifthUser", "sixthUser"};

		checkSortedSearchResultsOrder(
			"Smith", expected, SINGLE_LONG, Sort.LONG_TYPE);
	}

	protected SearchContext buildSearchContext(String keywords)
		throws Exception {

		SearchContext searchContext = ServiceTestUtil.getSearchContext();

		searchContext.setGroupIds(new long[] {});
		searchContext.setKeywords(keywords);
		searchContext.setAttribute(Field.STATUS, WorkflowConstants.STATUS_ANY);

		return searchContext;
	}

	protected void checkNumberOfSearchResults(
			SearchContext searchContext, long expectedResults)
		throws Exception {

		Hits results = _indexer.search(searchContext);

		Assert.assertEquals(expectedResults, results.getLength());
	}

	protected void checkSearchResultsData(Hits results) {
		for (Document doc : results.getDocs()) {
			String screenName = doc.get("screenName");

			Assert.assertEquals(
				Double.valueOf(doc.get(SINGLE_DOUBLE)),
				_userSingleDouble.get(screenName), 0);

			Assert.assertEquals(
				Long.valueOf(doc.get(SINGLE_LONG)),
				_userSingleLong.get(screenName), 0);

			Assert.assertEquals(
				Float.valueOf(doc.get(SINGLE_FLOAT)),
				_userSingleFloat.get(screenName), 0);

			Assert.assertEquals(
				Integer.valueOf(doc.get(SINGLE_INT)),
				_userSingleInteger.get(screenName), 0);

			Assert.assertArrayEquals(
				getMultiDouble(doc), _userMultiDouble.get(screenName));

			Assert.assertArrayEquals(
				getMultiLong(doc), _userMultiLong.get(screenName));

			Assert.assertArrayEquals(
				getMultiFloat(doc), _userMultiFloat.get(screenName));

			Assert.assertArrayEquals(
				getMultiInteger(doc), _userMultiInteger.get(screenName));
		}
	}

	protected void checkSortedSearchResultsOrder(
			SearchContext searchContext, Sort sort, String[] expected)
		throws Exception {

		Query query = _indexer.getFullQuery(searchContext);

		Hits results = SearchEngineUtil.search(
			searchContext.getSearchEngineId(), searchContext.getCompanyId(),
			query, sort, QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		Assert.assertEquals(expected.length, results.getLength());

		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals(expected[i], results.doc(i).get("screenName"));
		}

		checkSearchResultsData(results);
	}

	protected void checkSortedSearchResultsOrder(
			String keywords, String[] expectedAsc, String field, int type)
		throws Exception {

		String[] expectedDsc = Arrays.copyOf(expectedAsc, expectedAsc.length);

		ArrayUtil.reverse(expectedDsc);

		SearchContext searchContext = buildSearchContext(keywords);

		Sort sort = SortFactoryUtil.create(field, type, false);

		checkSortedSearchResultsOrder(searchContext, sort, expectedAsc);

		checkSortedSearchResultsOrder(searchContext, sort, expectedDsc);
	}

	protected Double[] getMultiDouble(Document doc) {
		List<Double> multiDouble = new ArrayList<Double>();

		for (String value : doc.getValues(MULTI_DOUBLE)) {
			multiDouble.add(Double.valueOf(value));
		}

		return multiDouble.toArray(new Double[]{});
	}

	protected Float[] getMultiFloat(Document doc) {
		List<Float> multiFloat = new ArrayList<Float>();

		for (String value : doc.getValues(MULTI_FLOAT)) {
			multiFloat.add(Float.valueOf(value));
		}

		return multiFloat.toArray(new Float[]{});
	}

	protected Integer[] getMultiInteger(Document doc) {
		List<Integer> multiInt = new ArrayList<Integer>();

		for (String value : doc.getValues(MULTI_INT)) {
			multiInt.add(Integer.valueOf(value));
		}

		return multiInt.toArray(new Integer[]{});
	}

	protected Long[] getMultiLong(Document doc) {
		List<Long> multiLong = new ArrayList<Long>();

		for (String value : doc.getValues(MULTI_LONG)) {
			multiLong.add(Long.valueOf(value));
		}

		return multiLong.toArray(new Long[]{});
	}

	protected void populateMultiUserNumbers(
		String screenName, Double[] numberDoubles, Long[] numberLongs) {

		Float[] numberFloats = new Float[numberDoubles.length];
		Integer[] numberIntegers = new Integer[numberLongs.length];

		for (int i = 0; i < numberDoubles.length; i++) {
			double numberDouble = numberDoubles[i];

			numberFloats[i] = new Float((float)numberDouble);
		}

		for (int i = 0; i < numberLongs.length; i++) {
			long numberLong = numberLongs[i];

			numberIntegers[i] = Long.valueOf(numberLong).intValue();
		}

		_userMultiDouble.put(screenName, numberDoubles);

		_userMultiFloat.put(screenName, numberFloats);

		_userMultiInteger.put(screenName, numberIntegers);

		_userMultiLong.put(screenName, numberLongs);
	}

	protected void populateSingleUserNumbers(
		String screenName, Double numberDouble, Long numberLong) {

		_userSingleDouble.put(screenName, numberDouble);

		_userSingleFloat.put(screenName, numberDouble.floatValue());

		_userSingleInteger.put(screenName, numberLong.intValue());

		_userSingleLong.put(screenName, numberLong);
	}

	protected void populateUsersNumbers() {
		populateSingleUserNumbers(
			"firstUser", 0.0000000000001, new Long(Integer.MIN_VALUE - 9L));
		populateMultiUserNumbers(
			"firstUser", new Double[] {0.0000000000001, 0.0000000000002},
			new Long[] {(Integer.MIN_VALUE - 9L), (Integer.MIN_VALUE - 8L)});

		populateSingleUserNumbers(
			"secondUser", 0.0000020000002, new Long(Integer.MIN_VALUE - 8L));
		populateMultiUserNumbers(
			"secondUser", new Double[]{0.0000020000003, 0.0000020000004},
			new Long[]{(Integer.MIN_VALUE - 7L), (Integer.MIN_VALUE - 6L)});

		populateSingleUserNumbers(
			"thirdUser", 0.0000000000003, new Long(Integer.MIN_VALUE - 7L));
		populateMultiUserNumbers(
			"thirdUser", new Double[]{0.0000000000003, 0.0000000000004},
			new Long[]{(Integer.MIN_VALUE - 5L), (Integer.MIN_VALUE - 4L)});

		populateSingleUserNumbers(
			"fourthUser", 0.0000040000004, new Long(Integer.MIN_VALUE + 7L));
		populateMultiUserNumbers(
			"fourthUser", new Double[]{0.0000040000004, 0.0000040000005},
			new Long[]{(Integer.MIN_VALUE + 7L), (Integer.MIN_VALUE + 8L)});

		populateSingleUserNumbers(
			"fifthUser", 0.0000000000005, new Long(Integer.MIN_VALUE + 8L));
		populateMultiUserNumbers(
			"fifthUser", new Double[]{0.0000000000005, 0.0000000000006},
			new Long[]{(Integer.MIN_VALUE + 9L), (Integer.MIN_VALUE + 10L)});

		populateSingleUserNumbers(
			"sixthUser", 0.0000060000006, new Long(Integer.MIN_VALUE + 9L));
		populateMultiUserNumbers(
			"sixthUser", new Double[]{0.0000060000006, 0.0000060000007},
			new Long[]{(Integer.MIN_VALUE + 11L), (Integer.MIN_VALUE + 12L)});
	}

	protected Indexer _indexer;

	protected Map<String, Double[]> _userMultiDouble =
		new HashMap<String, Double[]>();
	protected Map<String, Float[]> _userMultiFloat =
		new HashMap<String, Float[]>();
	protected Map<String, Integer[]> _userMultiInteger =
			new HashMap<String, Integer[]>();
	protected Map<String, Long[]> _userMultiLong =
		new HashMap<String, Long[]>();

	protected Map<String, Double> _userSingleDouble =
		new HashMap<String, Double>();
	protected Map<String, Float> _userSingleFloat =
		new HashMap<String, Float>();
	protected Map<String, Integer> _userSingleInteger =
		new HashMap<String, Integer>();
	protected Map<String, Long> _userSingleLong = new HashMap<String, Long>();

	protected class ExtendedUserIndexerPostProcessor extends
		BaseIndexerPostProcessor {

		@Override
		public void postProcessDocument(Document document, Object obj)
			throws Exception {

			String screenName = document.get("screenName");

			document.addNumber(MULTI_DOUBLE, _userMultiDouble.get(screenName));
			document.addNumber(MULTI_FLOAT, _userMultiFloat.get(screenName));
			document.addNumber(MULTI_INT, _userMultiInteger.get(screenName));
			document.addNumber(MULTI_LONG, _userMultiLong.get(screenName));
			document.addNumber(
				SINGLE_DOUBLE, _userSingleDouble.get(screenName));
			document.addNumber(SINGLE_FLOAT, _userSingleFloat.get(screenName));
			document.addNumber(SINGLE_INT, _userSingleInteger.get(screenName));
			document.addNumber(SINGLE_LONG, _userSingleLong.get(screenName));
		}
	}

}