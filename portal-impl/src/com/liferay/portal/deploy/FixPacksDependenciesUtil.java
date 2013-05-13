/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.deploy;

import com.liferay.portal.kernel.deploy.auto.context.AutoDeploymentContext;
import com.liferay.portal.kernel.patcher.PatcherUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnmodifiableList;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Daniel Sanz
 */
public class FixPacksDependenciesUtil {
	public static boolean checkFixPackDependencies(
			AutoDeploymentContext autoDeploymentContext,
			String propertiesString)
		throws Exception {

		Properties properties = new Properties();

		properties.load(new StringReader(propertiesString));

		return checkFixPackDependencies(
			autoDeploymentContext, properties);
	}

	public static boolean checkFixPackDependencies(
			AutoDeploymentContext autoDeploymentContext,
			Properties properties) {

		boolean meetsDependencies = true;

		if (properties.containsKey(REQUIRED_FIXES)) {
			String requiredFixes = properties.getProperty(REQUIRED_FIXES);

			String[] requiredFixesArray = StringUtil.split(requiredFixes);

			List<String> missing = getMissingFixPacks(
				PatcherUtil.getFixedIssues(), requiredFixesArray);

			autoDeploymentContext.setRequiredFixes(requiredFixesArray);
			autoDeploymentContext.setMissingFixes(null);

			if (missing.size() > 0) {
				autoDeploymentContext.setMissingFixes(
					missing.toArray(new String[missing.size()]));

				meetsDependencies = false;
			}
		}

		return meetsDependencies;
	}

	public static void registerMissingDeployment(
		AutoDeploymentContext autoDeploymentContext) {

		if (!_missingDeployments.contains(autoDeploymentContext)) {
			processMissing(autoDeploymentContext);

			_missingDeployments.add(autoDeploymentContext);
		}
	}

	public static void unregisterMissingDeployment(
		AutoDeploymentContext autoDeploymentContext) {

		if (_missingDeployments.contains(autoDeploymentContext)) {
			_missingDeployments.remove(autoDeploymentContext);
		}
	}

	public static List<AutoDeploymentContext> getMissingDeployments() {
		return new UnmodifiableList<AutoDeploymentContext>(_missingDeployments);
	}

	protected static List<String> getMissingFixPacks(
		String[] installed, String[] required) {

		List<String> installedList = Arrays.asList(installed);

		List<String> missing = new ArrayList<String>();

		for (String requiredFix : required) {
			if (!installedList.contains(requiredFix)) {
				missing.add(requiredFix);
			}
		}

		return missing;
	}

	protected static void processMissing(
		AutoDeploymentContext autoDeploymentContext) {

		String sourceFile = autoDeploymentContext.getFile().getAbsolutePath();

		String destFile =
			sourceFile + StringPool.PERIOD + UNMET_DEPENDENCIES_EXTENSION;

		try {
			FileUtil.copyFile(sourceFile, destFile);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String REQUIRED_FIXES="required-fixes";
	private static List<AutoDeploymentContext> _missingDeployments =
		new ArrayList<AutoDeploymentContext>();
	private static String UNMET_DEPENDENCIES_EXTENSION="unmet";
}