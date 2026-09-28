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
 * Answers what a configuration renders, and what it renders dishonestly.
 *
 * <p>
 * Both answers are pure functions of configuration: no image, no request and no
 * deployed renderer participate, so a whole configuration can be checked when
 * it is edited rather than one placement at a time when it is served. That
 * matters because the standing hazard of responsive images is silence — a
 * preset describing the wrong layout still renders, just wrongly, and reports
 * nothing.
 * </p>
 *
 * <p>
 * The mistake this deliberately does not look for is the serious one:
 * differently cropped candidates inside a single <code>srcset</code>, which
 * asks the browser to treat two different pictures as interchangeable.
 * It is unreachable rather than unchecked. A transformation is declared on a
 * preset, a preset carries one media condition, and the candidates generated
 * within one preset differ only in width, so candidates that share a
 * <code>srcset</code> are the same picture at different sizes by construction.
 * Allowing transformations per candidate would end that, and would make this the
 * place to catch it.
 * </p>
 *
 * <p>
 * Problems are reported as text because the only consumer is the log. A
 * configuration UI wanting to place them against the field that caused them
 * would want a structured type instead.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ResponsiveImageConfigurationValidator {

	/**
	 * Returns the element the given preset renders as.
	 *
	 * @param  presetDefinition the preset
	 * @return the element
	 */
	public static MarkupShape getMarkupShape(
		PresetDefinition presetDefinition) {

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		if (sourceDefinitions.size() > 1) {
			return MarkupShape.PICTURE;
		}

		return MarkupShape.IMG;
	}

	/**
	 * Returns one message per problem found, or an empty list.
	 *
	 * @param  presetDefinitions the presets parsed from configuration
	 * @return the problems
	 */
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

	/**
	 * Returns the format selecting transformations of a preset, which are the
	 * ones a media condition is the wrong way to choose between.
	 */
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

	/**
	 * Returns whether every preset generates the same candidates, in which case
	 * the sources they render are substitutable and the preset had no reason to
	 * be art directed.
	 *
	 * <p>
	 * A differing maximum width is not counted, because it truncates the ladder
	 * per source and a single <code>&lt;img&gt;</code> carries one ladder.
	 * Differing sizes alone is the case worth reporting: one sizes attribute
	 * already holds a media condition per entry.
	 * </p>
	 */
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