/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.organizations.item.selector.util;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringUtil;

import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author Daniel Sanz
 */
public class OrganizationsItemSelectorCriterionUtil {

	public static long[] toLongArray(String selectedOrganizationIds) {
		String decompressed = _decompress(selectedOrganizationIds);

		List<String> ids = StringUtil.split(decompressed, '-');

		return TransformUtil.transformToLongArray(ids, Long::parseLong);
	}

	public static String toString(long[] selectedOrganizationIds) {
		String merged = StringUtil.merge(selectedOrganizationIds, "-");

		return _compress(merged);
	}

	private static String _compress(String value) {
		byte[] input = value.getBytes(StandardCharsets.UTF_8);

		Deflater deflater = new Deflater();

		deflater.setInput(input);
		deflater.finish();

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
			input.length);

		byte[] buffer = new byte[1024];

		while (!deflater.finished()) {
			int count = deflater.deflate(buffer);

			byteArrayOutputStream.write(buffer, 0, count);
		}

		deflater.end();

		return Base64.getUrlEncoder(
		).withoutPadding(
		).encodeToString(
			byteArrayOutputStream.toByteArray()
		);
	}

	private static String _decompress(String value) {
		try {
			byte[] compressed = Base64.getUrlDecoder(
			).decode(
				value
			);

			Inflater inflater = new Inflater();

			inflater.setInput(compressed);

			ByteArrayOutputStream byteArrayOutputStream =
				new ByteArrayOutputStream(compressed.length);

			byte[] buffer = new byte[1024];

			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);

				byteArrayOutputStream.write(buffer, 0, count);
			}

			inflater.end();

			return byteArrayOutputStream.toString(
				StandardCharsets.UTF_8.name());
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

}