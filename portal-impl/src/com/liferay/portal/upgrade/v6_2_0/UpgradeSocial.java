/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.portal.upgrade.v6_2_0;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.blogs.social.BlogsActivityKeys;
import com.liferay.portlet.messageboards.social.MBActivityKeys;
import com.liferay.portlet.social.model.SocialActivityConstants;
import com.liferay.wiki.social.WikiActivityKeys;
import com.liferay.bookmarks.social.BookmarksActivityKeys;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sergio Sanchez
 * @author Zsolt Berentey
 */
public class UpgradeSocial extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		updateJournalActivities();
		updateSOSocialActivities();
		updateActivities();
	}

	protected Map<Long, String> generateExtraData(
			ExtraDataGenerator extraDataGenerator)
		throws Exception {

		Map<Long, String> extraDataMap = new HashMap<Long, String>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			StringBundler sb = new StringBundler(4);

			sb.append("select activityId, groupId, companyId, userId, ");
			sb.append("classNameId, classPK, type_, extraData ");
			sb.append("from SocialActivity where ");
			sb.append(extraDataGenerator.getActivityQueryWhereClause());

			ps = con.prepareStatement(sb.toString());

			extraDataGenerator.setActivityQueryParameters(ps);

			rs = ps.executeQuery();

			while (rs.next()) {
				long activityId = rs.getLong("activityId");
				long classNameId = rs.getLong("classNameId");
				long classPK = rs.getLong("classPK");
				long companyId = rs.getLong("companyId");
				String extraData = rs.getString("extraData");
				long groupId = rs.getLong("groupId");
				int type = rs.getInt("type_");
				long userId = rs.getLong("userId");

				String newExtraData = generateExtraDataForActivity(
					extraDataGenerator, groupId, companyId, userId, classNameId,
					classPK, type, extraData);

				if (newExtraData != null) {
					extraDataMap.put(activityId, newExtraData);
				}
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
		return extraDataMap;
	}

	protected String generateExtraDataForActivity(
			ExtraDataGenerator extraDataGenerator, long companyId, long groupId,
			long userId, long classNameId, long classPK, int type,
			String extraData)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String result = null;

		try {
			if (extraDataGenerator != null) {
				con = DataAccess.getUpgradeOptimizedConnection();

				ps = con.prepareStatement(extraDataGenerator.getEntityQuery());

				extraDataGenerator.setEntityQueryParameters(
					ps, groupId, companyId, userId, classNameId, classPK, type,
					extraData);

				rs = ps.executeQuery();

				JSONObject extraDataJSONObject = null;

				while (rs.next()) {
					extraDataJSONObject =
						extraDataGenerator.getExtraData(rs, extraData);
				}

				result = extraDataJSONObject.toString();
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return result;
	}

	protected void updateActivities() throws Exception {
		for (ExtraDataGenerator extraDataGenerator : _extraDataGenerators) {
			updateActivities(extraDataGenerator);
		}
	}

	protected void updateActivities(ExtraDataGenerator extraDataGenerator)
			throws Exception {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		Map<Long, String> extraDataMap = generateExtraData(extraDataGenerator);

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			StringBundler sb = new StringBundler(2);

			sb.append("update SocialActivity set extraData = ? ");
			sb.append("where activityId = ?");

			String updateActivityQuery = sb.toString();

			for (Map.Entry<Long, String> entry : extraDataMap.entrySet()) {
				long activityId = entry.getKey();
				String extraData = entry.getValue();
				try {
					ps = con.prepareStatement(updateActivityQuery);

					ps.setString(1, extraData);
					ps.setLong(2, activityId);

					ps.executeUpdate();
				}
				catch (Exception e) {
					if (_log.isWarnEnabled()) {
						_log.warn("Unable to update activity " + activityId, e);
					}
				}
			}
		}

		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void updateJournalActivities() throws Exception {
		long classNameId = PortalUtil.getClassNameId(
			"com.liferay.portlet.journal.model.JournalArticle");


	}

	protected void updateSOSocialActivities() throws Exception {
		if (!hasTable("SO_SocialActivity")) {
			return;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"select activityId, activitySetId from SO_SocialActivity");

			rs = ps.executeQuery();

			while (rs.next()) {
				long activityId = rs.getLong("activityId");
				long activitySetId = rs.getLong("activitySetId");

				StringBundler sb = new StringBundler(4);

				sb.append("update SocialActivity set activitySetId = ");
				sb.append(activitySetId);
				sb.append(" where activityId = ");
				sb.append(activityId);

				runSQL(sb.toString());
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		runSQL("drop table SO_SocialActivity");
	}

	protected Map<Long, String> _extraDataMap = new HashMap<Long, String>();
protected static final List<ExtraDataGenerator>
		_extraDataGenerators = new ArrayList<ExtraDataGenerator>();

	protected abstract static class  ExtraDataGenerator {
		/**
		 * Returns the "where" clause in SocialActivity query to select the
		 * activities for which this generator will generate extra data
		 */
		public abstract String getActivityQueryWhereClause();

		/**
		 * Returns the query on any model entity which the selected activities
		 * refer to. Extra data will be generated from these entities
		 */
		public abstract String  getEntityQuery();

		/**
		 * Given a result from the #getEntityQuery() and the original extra
		 * data in the SocialActivity tuple pointing to that entity, computes
		 * the extra data that will be persisted in the SocialActivity tuple as
		 * a result of the upgrade process.
		 *
		 * @return JSONObject containing the extra data
		 */
		public abstract JSONObject getExtraData(
				ResultSet entityResultSet, String extraData)
			throws SQLException;

		/**
		 * Sets parameters required to run the activity query returned by
		 * #getActivityQueryWhereClause() in this generator
		 */
		public abstract void setActivityQueryParameters(PreparedStatement ps)
			throws SQLException;

		/**
		 * Sets parameters required to run the entity query returned by
		 * #getEntityQueryWhereClause() in this generator
		 */
		public abstract void setEntityQueryParameters(PreparedStatement ps,
				long companyId, long groupId, long userId, long classNameId,
				long classPK, int type, String extraData)
			throws SQLException;
	}

	private static final Log _log = LogFactoryUtil.getLog(UpgradeSocial.class);

	private static final ExtraDataGenerator _dlFileEntryExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "classNameId = ?";
			}

			public String getEntityQuery() {
				return "select title from DLFileEntry where" +
					" companyId = ? and groupId = ? and fileEntryId = ?";
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setLong(1, PortalUtil.getClassNameId(
					"com.liferay.portlet.documentlibrary.model.DLFileEntry"));
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				ps.setLong(1, companyId);
				ps.setLong(2, groupId);
				ps.setLong(3, classPK);
			}

			public JSONObject getExtraData(
				ResultSet entityResultSet, String extraData)
					throws SQLException {

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("title", entityResultSet.getString("title"));

				return result;
			}
		};

	private static final ExtraDataGenerator _wikiPageExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "classNameId = ? and ( type_= ? or type_ = ?)";
			}

			public String getEntityQuery() {
				return "select title, version from WikiPage where" +
					" companyId = ? and groupId = ? and resourcePrimKey = ? " +
					" and head = true";
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				ps.setLong(1, companyId);
				ps.setLong(2, groupId);
				ps.setLong(3, classPK);
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setLong(1, PortalUtil.getClassNameId(
					"com.liferay.wiki.model.WikiPage"));

				ps.setInt(2, WikiActivityKeys.ADD_PAGE);

				ps.setInt(3, WikiActivityKeys.UPDATE_PAGE);
			}

			public JSONObject getExtraData(
					ResultSet entityResultSet, String extraData)
				throws SQLException {

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("title", entityResultSet.getString("title"));
				result.put("version", entityResultSet.getDouble("version"));

				return result;
			}
		};

	private static final ExtraDataGenerator _addAssetCommentExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "type_= ?";
			}

			public String getEntityQuery() {
				return "select subject from MBMessage where messageId = ?";
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				long messageId = 0;

				try {
					JSONObject extraDataJson =
						JSONFactoryUtil.createJSONObject(extraData);

					messageId = extraDataJson.getLong("messageId");
				}
				catch (JSONException e) {
				}

				ps.setLong(1, messageId);
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setInt(1, SocialActivityConstants.TYPE_ADD_COMMENT);
			}

			public JSONObject getExtraData(
					ResultSet entityResultSet, String extraData)
				throws SQLException {

				long messageId = 0;

				try {
					JSONObject extraDataJson =
						JSONFactoryUtil.createJSONObject(extraData);

					messageId = extraDataJson.getLong("messageId");
				}
				catch (JSONException e) {
				}

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("messageId", messageId);
				result.put("title", entityResultSet.getString("subject"));

				return result;
			}
		};

	private static final ExtraDataGenerator _addMessageExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "classNameId = ? and (type_= ? or type_ = ?)";
			}

			public String getEntityQuery() {
				return "select subject from MBMessage where messageId = ?";
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				ps.setLong(1, classPK);
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setLong(1, PortalUtil.getClassNameId(
					"com.liferay.portlet.messageboards.model.MBMessage"));

				ps.setInt(2, MBActivityKeys.ADD_MESSAGE);

				ps.setInt(3, MBActivityKeys.REPLY_MESSAGE);
			}

			public JSONObject getExtraData(
					ResultSet entityResultSet, String extraData)
				throws SQLException {

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("title", entityResultSet.getString("subject"));

				return result;
			}
		};

	private static final ExtraDataGenerator _blogsEntryExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "classNameId = ? and (type_= ? or type_ = ?)";
			}

			public String getEntityQuery() {
				return "select title from BlogsEntry where entryId = ?";
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				ps.setLong(1, classPK);
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setLong(1, PortalUtil.getClassNameId(
					"com.liferay.portlet.blogs.model.BlogsEntry"));

				ps.setInt(2, BlogsActivityKeys.ADD_ENTRY);

				ps.setInt(3, BlogsActivityKeys.UPDATE_ENTRY);
			}

			public JSONObject getExtraData(
					ResultSet entityResultSet, String extraData)
				throws SQLException {

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("title", entityResultSet.getString("title"));

				return result;
			}
		};

	private static final ExtraDataGenerator _bookmarksEntryExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "classNameId = ? and (type_= ? or type_ = ?)";
			}

			public String getEntityQuery() {
				return "select name from BookmarksEntry where entryId = ?";
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				ps.setLong(1, classPK);
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setLong(1, PortalUtil.getClassNameId(
					"com.liferay.portlet.bookmarks.model.BookmarksEntry"));

				ps.setInt(2, BookmarksActivityKeys.ADD_ENTRY);

				ps.setInt(3, BookmarksActivityKeys.UPDATE_ENTRY);
			}

			public JSONObject getExtraData(
					ResultSet entityResultSet, String extraData)
				throws SQLException {

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("title", entityResultSet.getString("name"));

				return result;
			}
		};

	private static final ExtraDataGenerator _kbArticleExtraDataGenerator =
		new ExtraDataGenerator() {
			public String getActivityQueryWhereClause() {
				return "classNameId = ? and (type_= ? or type_ = ?)";
			}

			public String getEntityQuery() {
				return "select name from BookmarksEntry where entryId = ?";
			}

			public void setEntityQueryParameters(
					PreparedStatement ps, long companyId, long groupId,
					long userId, long classNameId, long classPK, int type,
					String extraData)
				throws SQLException {

				ps.setLong(1, classPK);
			}

			public void setActivityQueryParameters(PreparedStatement ps)
				throws SQLException {

				ps.setLong(1, PortalUtil.getClassNameId(
					"com.liferay.portlet.bookmarks.model.BookmarksEntry"));

				ps.setInt(2, BookmarksActivityKeys.ADD_ENTRY);

				ps.setInt(3, BookmarksActivityKeys.UPDATE_ENTRY);
			}

			public JSONObject getExtraData(
					ResultSet entityResultSet, String extraData)
				throws SQLException {

				JSONObject result = JSONFactoryUtil.createJSONObject();

				result.put("title", entityResultSet.getString("name"));

				return result;
			}
		};

	public void doTest() throws Exception {
			for (ExtraDataGenerator extraDataGenerator : _extraDataGenerators) {
				Map<Long, String> extraDataMap =
						generateExtraData(extraDataGenerator);

				_log.info("");
				_log.info("Extra Data Generator: " + extraDataGenerator.getEntityQuery());

				for (Map.Entry<Long, String> entry : extraDataMap.entrySet()) {
					long activityId = entry.getKey();
					String extraData = entry.getValue();

					_log.info("ActivityId: " + activityId + ". Extradata: " +
							  extraData);
				}
			}
		}

	com.liferay.knowledgebase.model.KBArticle

	static {
		_extraDataGenerators.add(_addAssetCommentExtraDataGenerator);
		_extraDataGenerators.add(_addMessageExtraDataGenerator);
		_extraDataGenerators.add(_blogsEntryExtraDataGenerator);
		_extraDataGenerators.add(_bookmarksEntryExtraDataGenerator);
		_extraDataGenerators.add(_dlFileEntryExtraDataGenerator);
		_extraDataGenerators.add(_wikiPageExtraDataGenerator);
	}
}

new UpgradeSocial().doTest();
