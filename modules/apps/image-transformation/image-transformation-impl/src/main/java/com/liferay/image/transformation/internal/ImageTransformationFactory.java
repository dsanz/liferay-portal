/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.util.Portal;

import java.util.List;
import java.util.function.Supplier;

/**
 * Assembles the collaborators shared by this bundle's components.
 *
 * <p>
 * They are plain objects rather than OSGi components on purpose. Neither
 * implements an interface, so registering one as a service means naming its own
 * class in the <code>service</code> attribute, which reads as redundant and is
 * easy to "simplify" into <code>service = {}</code> — leaving the component
 * active but invisible and every reference to it unsatisfied. Constructing them
 * from collaborators the consumer already injects removes that failure mode.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ImageTransformationFactory {

	public static ImageTransformationConfigurationHelper
		createImageTransformationConfigurationHelper(
			ConfigurationProvider configurationProvider, Portal portal) {

		return new ImageTransformationConfigurationHelper(
			configurationProvider, portal);
	}

	/**
	 * @param imageTransformationProvidersSupplier read on each selection rather
	 *        than captured, so that providers appearing or disappearing at
	 *        runtime are seen without rebuilding the selector
	 */
	public static ImageTransformationProviderSelector
		createImageTransformationProviderSelector(
			ImageTransformationConfigurationHelper
				imageTransformationConfigurationHelper,
			Supplier<List<ImageTransformationProvider>>
				imageTransformationProvidersSupplier) {

		return new ImageTransformationProviderSelector(
			imageTransformationConfigurationHelper,
			imageTransformationProvidersSupplier);
	}

}