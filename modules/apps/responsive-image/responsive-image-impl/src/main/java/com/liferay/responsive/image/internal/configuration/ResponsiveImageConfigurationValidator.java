/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import com.liferay.petra.string.StringBundler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Daniel Sanz
 */
public class ResponsiveImageConfigurationValidator {

	public static List<String> validate(
		Map<String, PresetDefinition> presetDefinitions) {

		List<String> problems = new ArrayList<>();

		for (PresetDefinition presetDefinition : presetDefinitions.values()) {
			List<SourceDefinition> sourceDefinitions =
				presetDefinition.getSourceDefinitions();

			if (sourceDefinitions.size() < 2) {
				continue;
			}

			if (_isInterchangeable(sourceDefinitions)) {
				problems.add(
					StringBundler.concat(
						"Preset ", presetDefinition.getName(), " declares ",
						sourceDefinitions.size(),
						" media conditions that generate the same image, so ",
						"it renders a <picture> whose sources are ",
						"interchangeable. Declare a single unconditional ",
						"preset and move the media conditions into its sizes ",
						"attribute, which renders an <img> and leaves the ",
						"choice to the browser"));
			}

			if (_isFormatSwitching(sourceDefinitions)) {
				problems.add(
					StringBundler.concat(
						"Preset ", presetDefinition.getName(),
						" changes output format between media conditions, ",
						"which a <source> cannot express: it is selected by ",
						"media condition alone, so a browser that does not ",
						"support the format has nothing to fall back to. ",
						"Negotiate the format at the edge from the Accept ",
						"header instead"));
			}
		}

		return problems;
	}

	private static Map<String, String> _getFormats(
		SourceDefinition sourceDefinition) {

		Map<String, String> formats = new LinkedHashMap<>();

		Map<String, String> transformations =
			sourceDefinition.getTransformations();

		for (String formatKey : _FORMAT_KEYS) {
			String value = transformations.get(formatKey);

			if (value != null) {
				formats.put(formatKey, value);
			}
		}

		return formats;
	}

	private static boolean _isFormatSwitching(
		List<SourceDefinition> sourceDefinitions) {

		Map<String, String> formats = _getFormats(sourceDefinitions.get(0));

		for (SourceDefinition sourceDefinition : sourceDefinitions) {
			if (!formats.equals(_getFormats(sourceDefinition))) {
				return true;
			}
		}

		return false;
	}

	private static boolean _isInterchangeable(
		List<SourceDefinition> sourceDefinitions) {

		SourceDefinition firstSourceDefinition = sourceDefinitions.get(0);

		Integer maxWidth = firstSourceDefinition.getMaxWidth();
		Map<String, String> transformations =
			firstSourceDefinition.getTransformations();

		for (SourceDefinition sourceDefinition : sourceDefinitions) {
			if (!Objects.equals(maxWidth, sourceDefinition.getMaxWidth()) ||
				!transformations.equals(
					sourceDefinition.getTransformations())) {

				return false;
			}
		}

		return true;
	}

	private static final String[] _FORMAT_KEYS = {"auto", "format"};

}