/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageCandidate;
import com.liferay.responsive.image.ResponsiveImageMarkupRenderer;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageSource;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry;

import java.util.List;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Turns the active provider's renditions into an image tag.
 *
 * <p>
 * Selects the provider, asks it for the model, and renders it. A provider that
 * renders its own markup says so by returning something from {@link
 * ResponsiveImageProvider#render}; everything else is rendered here, from
 * the model alone, so that adding a provider does not mean writing markup
 * again.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ResponsiveImageMarkupRenderer.class)
public class ResponsiveImageMarkupRendererImpl
	implements ResponsiveImageMarkupRenderer {

	/**
	 * Renders a single <code>&lt;img&gt;</code> when nothing was art
	 * directed, and <code>&lt;picture&gt;</code> when something was.
	 *
	 * <p>
	 * The shape follows from the sources themselves: a media condition on any
	 * of them means <code>&lt;picture&gt;</code>, because that is exactly
	 * when there is a <code>&lt;source&gt;</code> worth emitting.
	 * </p>
	 *
	 * <p>
	 * Returns <code>null</code> whenever anything is missing or no URL was
	 * actually transformed, so that a caller outranking Adaptive Media's own
	 * renderer can tell that it has to hand over.
	 * </p>
	 */
	@Override
	public String render(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		if (responsiveImageRequest == null) {
			return null;
		}

		// The cut for the whole feature. Off returns the tag the caller already
		// had, which is what it would have rendered before any of this existed.
		// Checked per company rather than per deployment, because an OSGi
		// registration is global while enabling a feature is not.

		if (!FeatureFlagManagerUtil.isEnabled(
				_responsiveImageConfigurationHelper.getCompanyId(
					responsiveImageRequest),
				"LPD-94784")) {

			return null;
		}

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		if (imageResource == null) {
			return null;
		}

		ResponsiveImage responsiveImage = _responsiveImageFactory.create(
			responsiveImageRequest);

		if (responsiveImage == null) {
			return null;
		}

		List<ResponsiveImageSource> responsiveImageSources =
			responsiveImage.getSources();

		if (responsiveImageSources.isEmpty() ||
			_hasCandidatesWithSameURL(responsiveImageSources)) {

			// Nothing was generated, or a renderer that is deployed could not
			// apply the transformations and handed every width the same URL
			// back, which its contract allows. Emitting the descriptors anyway
			// would have the browser trust them and render at the wrong
			// density.

			return null;
		}

		if (_requiresPictureElement(responsiveImageSources)) {
			return _renderPicture(
				responsiveImageSources, responsiveImage.isLazy(),
				originalImgTag);
		}

		return _renderImg(
			responsiveImageSources.get(0), responsiveImage.isLazy(),
			originalImgTag);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerList = ServiceTrackerListFactory.open(
			bundleContext, ImageTransformationURLRenderer.class);

		_responsiveImageConfigurationHelper =
			new ResponsiveImageConfigurationHelper(
				_configurationProvider, _portal);

		_responsiveImageFactory = new ResponsiveImageFactory(
			_absolutePortalURLBuilderFactory,
			new ResponsiveImageConfigurationRegistry(
				_configurationProvider, _portal),
			_responsiveImageConfigurationHelper, _serviceTrackerList::toList);
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerList.close();
	}

	private String _getSrcSet(
		List<ResponsiveImageCandidate> responsiveImageCandidates) {

		StringBundler sb = new StringBundler(
			responsiveImageCandidates.size() * 4);

		for (ResponsiveImageCandidate responsiveImageCandidate :
				responsiveImageCandidates) {

			if (responsiveImageCandidate.getWidth() == null) {
				continue;
			}

			if (sb.index() > 0) {
				sb.append(StringPool.COMMA_AND_SPACE);
			}

			sb.append(responsiveImageCandidate.getURL());
			sb.append(StringPool.SPACE);
			sb.append(responsiveImageCandidate.getWidth());
			sb.append("w");
		}

		return sb.toString();
	}

	/**
	 * Returns <code>true</code> if the widths never reached the URLs.
	 *
	 * <p>
	 * Two candidates in one source differ only by width, so identical URLs
	 * mean the width never reached the URL. Comparing within a source rather
	 * than against the original is what makes this work regardless of the CDN
	 * host and proxy path the builder prepends.
	 * </p>
	 *
	 * <p>
	 * What reaches this is a renderer that <b>is</b> deployed and returned the
	 * URL untouched, which {@code ImageTransformationURLRenderer#render}
	 * permits when it cannot apply any of the transformations. A scope with no
	 * renderer at all never gets this far: that is the feature being off, and
	 * it is answered before a ladder is built.
	 * </p>
	 *
	 * <p>
	 * A source holding a single candidate cannot be compared, so it passes.
	 * The descriptor may then be wrong, but there is nothing for a browser to
	 * choose between.
	 * </p>
	 */
	private boolean _hasCandidatesWithSameURL(
		List<ResponsiveImageSource> responsiveImageSources) {

		for (ResponsiveImageSource responsiveImageSource :
				responsiveImageSources) {

			List<ResponsiveImageCandidate> responsiveImageCandidates =
				responsiveImageSource.getCandidates();

			if (responsiveImageCandidates.size() < 2) {
				continue;
			}

			ResponsiveImageCandidate firstResponsiveImageCandidate =
				responsiveImageCandidates.get(0);
			ResponsiveImageCandidate secondResponsiveImageCandidate =
				responsiveImageCandidates.get(1);

			return Objects.equals(
				firstResponsiveImageCandidate.getURL(),
				secondResponsiveImageCandidate.getURL());
		}

		return false;
	}

	private String _injectAttributes(String imgTag, String attributes) {
		int i = imgTag.indexOf("<img");

		if (i == -1) {
			return imgTag;
		}

		return StringBundler.concat(
			imgTag.substring(0, i + 4), StringPool.SPACE, attributes,
			imgTag.substring(i + 4));
	}

	private String _renderImg(
		ResponsiveImageSource responsiveImageSource, boolean lazy,
		String originalImgTag) {

		StringBundler sb = new StringBundler(7);

		sb.append("srcset=\"");
		sb.append(
			HtmlUtil.escapeAttribute(
				_getSrcSet(responsiveImageSource.getCandidates())));
		sb.append("\"");

		String sizes = responsiveImageSource.getSizes();

		if (!Validator.isBlank(sizes)) {
			sb.append(" sizes=\"");
			sb.append(HtmlUtil.escapeAttribute(sizes));
			sb.append("\"");
		}

		// sizes="auto" is only honored on a lazily loaded image, so the two
		// attributes have to be emitted together or not at all.

		if (lazy && !originalImgTag.contains("loading=")) {
			sb.append(" loading=\"lazy\"");
		}

		return _injectAttributes(originalImgTag, sb.toString());
	}

	private String _renderPicture(
		List<ResponsiveImageSource> responsiveImageSources, boolean lazy,
		String originalImgTag) {

		StringBundler sb = new StringBundler(
			(responsiveImageSources.size() * 7) + 3);

		sb.append("<picture>");

		for (ResponsiveImageSource responsiveImageSource :
				responsiveImageSources) {

			String mediaQuery = responsiveImageSource.getMediaQuery();

			if (Validator.isBlank(mediaQuery)) {
				continue;
			}

			sb.append("<source media=\"");
			sb.append(HtmlUtil.escapeAttribute(mediaQuery));
			sb.append("\" srcset=\"");
			sb.append(
				HtmlUtil.escapeAttribute(
					_getSrcSet(responsiveImageSource.getCandidates())));
			sb.append("\"");

			String sizes = responsiveImageSource.getSizes();

			if (!Validator.isBlank(sizes)) {
				sb.append(" sizes=\"");
				sb.append(HtmlUtil.escapeAttribute(sizes));
				sb.append("\"");
			}

			sb.append(" />");
		}

		// The last source also feeds the img element, which is both the
		// fallback for browsers without picture support and the final source
		// when no media condition matches.

		sb.append(
			_renderImg(
				responsiveImageSources.get(responsiveImageSources.size() - 1),
				lazy, originalImgTag));

		sb.append("</picture>");

		return sb.toString();
	}

	/**
	 * Returns whether these sources need a <code>&lt;picture&gt;</code>.
	 *
	 * <p>
	 * True when any source carries a media condition, which is exactly when
	 * {@link #_renderPicture} has a <code>&lt;source&gt;</code> to emit: it
	 * skips the unconditional ones, so wrapping without one would produce a
	 * <code>&lt;picture&gt;</code> holding nothing but the image.
	 * </p>
	 *
	 * <p>
	 * Read from the sources rather than from the preset they were generated
	 * against. The preset says what was asked for; these are what came back,
	 * and they are what is about to be rendered.
	 * </p>
	 */
	private boolean _requiresPictureElement(
		List<ResponsiveImageSource> responsiveImageSources) {

		for (ResponsiveImageSource responsiveImageSource :
				responsiveImageSources) {

			if (!Validator.isBlank(responsiveImageSource.getMediaQuery())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Renders a single <code>&lt;img&gt;</code> when nothing was art
	 * directed, and <code>&lt;picture&gt;</code> when something was.
	 *
	 * <p>
	 * The shape comes from {@link
	 * ResponsiveImageConfigurationValidator#getMarkupShape}, which reads
	 * the configured presets rather than the sources that survived
	 * generation. A preset whose ladder came back empty is skipped, so counting
	 * sources instead would quietly downgrade an art directed placement
	 * to a plain <code>&lt;img&gt;</code> and drop the media conditions that
	 * made it art directed.
	 * </p>
	 *
	 * <p>
	 * Wrapping a lone source in <code>&lt;picture&gt;</code> would be pure
	 * overhead, and enumerating sources when only resolution varies would
	 * discard what the browser knows about pixel density, network conditions,
	 * and its own cache.
	 * </p>
	 */
	@Reference
	private AbsolutePortalURLBuilderFactory _absolutePortalURLBuilderFactory;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Portal _portal;

	private ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private ResponsiveImageFactory _responsiveImageFactory;
	private ServiceTrackerList<ImageTransformationURLRenderer>
		_serviceTrackerList;

}