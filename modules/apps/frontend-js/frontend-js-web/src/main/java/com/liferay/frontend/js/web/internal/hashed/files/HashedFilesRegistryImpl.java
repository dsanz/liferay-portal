/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.js.web.internal.hashed.files;

import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.frontend.hashed.files.HashedFilesRegistry;
import com.liferay.portal.kernel.frontend.hashed.files.HashedFilesUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import jakarta.servlet.ServletContext;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Iván Zaera Avellón
 */
@Component(service = HashedFilesRegistry.class)
public class HashedFilesRegistryImpl implements HashedFilesRegistry {

	public void forEach(BiConsumer<String, String> biConsumer) {
		_lazyActivate();

		for (Map.Entry<String, String> entry : _map.entrySet()) {
			biConsumer.accept(entry.getKey(), entry.getValue());
		}
	}

	public String getHashedFileURI(String unhashedFileURI) {
		_lazyActivate();

		return _map.get(unhashedFileURI);
	}

	@Override
	public String getImportMapAsString(String cdnHost) {
		_lazyActivate();

		String cdnHostKey = cdnHost;

		if (Validator.isNull(cdnHost)) {
			cdnHostKey = "null";
		}

		String importMap = _cdnHostMap.get(cdnHostKey);

		if (importMap != null) {
			return importMap;
		}

		StringBundler importMapSB = new StringBundler(_jsMap.size());

		if (Validator.isNull(cdnHost)) {
			for (Map.Entry<String, String> entry : _jsMap.entrySet()) {
				importMapSB.append(entry.getValue());
			}
		}
		else {
			for (Map.Entry<String, String> entry : _jsMap.entrySet()) {
				String key = entry.getKey();

				importMapSB.append(
					StringUtil.insert(
						entry.getValue(), cdnHost, key.length() + 7));
			}
		}

		importMap = importMapSB.toString();

		_cdnHostMap.put(cdnHostKey, importMap);

		return importMap;
	}

	@Override
	public URL getResource(String path) {
		_lazyActivate();

		int endIndex = path.indexOf(StringPool.SLASH, _startIndex + 1);

		ServletContext servletContext = _serviceTrackerMap.getService(
			path.substring(0, endIndex));

		if (servletContext == null) {
			return null;
		}

		try {
			return servletContext.getResource(path.substring(endIndex));
		}
		catch (MalformedURLException malformedURLException) {
			throw new RuntimeException(malformedURLException);
		}
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Deactivate
	protected void deactivate() {
		_cdnHostMap.clear();
		_jsMap.clear();
		_map.clear();

		if (_serviceTracker != null) {
			_serviceTracker.close();

			_serviceTracker = null;
		}

		if (_serviceTrackerMap != null) {
			_serviceTrackerMap.close();

			_serviceTrackerMap = null;
		}
	}

	private ServiceTrackerCustomizer<ServletContext, Map<String, String>>
		_createServiceTrackerCustomizer() {

		return new ServiceTrackerCustomizer<>() {

			@Override
			public Map<String, String> addingService(
				ServiceReference<ServletContext> serviceReference) {

				_cdnHostMap.clear();

				ServletContext servletContext = _bundleContext.getService(
					serviceReference);

				try {
					Set<String> hashedResourcePaths;

					URL url = servletContext.getResource(
						"/WEB-INF/liferay-look-and-feel.xml");

					if (url != null) {
						hashedResourcePaths = _getHashedResourcePaths(
							servletContext, "/css/");

						hashedResourcePaths.addAll(
							_getHashedResourcePaths(servletContext, "/js/"));
					}
					else {
						Set<String> completeHashedResourcePaths =
							_getHashedResourcePaths(
								servletContext, "/META-INF/resources/");

						hashedResourcePaths = new HashSet<>();

						for (String completeHashedResourcePath :
								completeHashedResourcePaths) {

							hashedResourcePaths.add(
								completeHashedResourcePath.substring(19));
						}
					}

					Map<String, String> map = new HashMap<>();

					Map<String, String> jsMap = new HashMap<>();

					String contextPath = servletContext.getContextPath();

					for (String hashedResourcePath : hashedResourcePaths) {
						String unhashedFileURI =
							contextPath +
								HashedFilesUtil.removeHash(hashedResourcePath);

						String hashedFileURI = contextPath + hashedResourcePath;

						map.put(unhashedFileURI, hashedFileURI);

						if (hashedResourcePath.endsWith(".js")) {
							StringBundler valueSB = new StringBundler(5);

							valueSB.append(", \"");
							valueSB.append(unhashedFileURI);
							valueSB.append("\": \"");
							valueSB.append(hashedFileURI);
							valueSB.append(StringPool.QUOTE);

							jsMap.put(unhashedFileURI, valueSB.toString());
						}
					}

					_jsMap.putAll(jsMap);

					_map.putAll(map);

					return map;
				}
				catch (MalformedURLException malformedURLException) {
					_log.error(malformedURLException);

					return Collections.emptyMap();
				}
				finally {
					_bundleContext.ungetService(serviceReference);
				}
			}

			@Override
			public void modifiedService(
				ServiceReference<ServletContext> serviceReference,
				Map<String, String> map) {

				removedService(serviceReference, map);

				addingService(serviceReference);
			}

			@Override
			public void removedService(
				ServiceReference<ServletContext> serviceReference,
				Map<String, String> map) {

				_cdnHostMap.clear();

				for (String key : map.keySet()) {
					_map.remove(key);

					_jsMap.remove(key);
				}
			}

		};
	}

	private Set<String> _getHashedResourcePaths(
		ServletContext servletContext, String folderPath) {

		Set<String> resourcePaths = servletContext.getResourcePaths(folderPath);

		if (resourcePaths == null) {
			return Collections.emptySet();
		}

		Set<String> hashedResourcePaths = new HashSet<>();

		for (String resourcePath : resourcePaths) {
			if (resourcePath.endsWith(StringPool.SLASH)) {
				hashedResourcePaths.addAll(
					_getHashedResourcePaths(servletContext, resourcePath));
			}
			else if (HashedFilesUtil.containsHash(resourcePath)) {
				hashedResourcePaths.add(resourcePath);
			}
		}

		return hashedResourcePaths;
	}

	private void _lazyActivate() {
		if (_startIndex == -1) {
			String proxyPath = _portal.getPathProxy();

			String contextPath = _portal.getPathContext();

			if (!proxyPath.isEmpty()) {
				contextPath = contextPath.substring(proxyPath.length() - 1);
			}

			_startIndex = contextPath.length() + 2;
		}

		if (_serviceTracker == null) {
			synchronized (this) {
				if (_serviceTracker == null) {
					_serviceTracker = new ServiceTracker<>(
						_bundleContext, ServletContext.class,
						_createServiceTrackerCustomizer());

					_serviceTracker.open();
				}
			}
		}

		if (_serviceTrackerMap == null) {
			synchronized (this) {
				if (_serviceTrackerMap == null) {
					_serviceTrackerMap =
						ServiceTrackerMapFactory.openSingleValueMap(
							_bundleContext, ServletContext.class, null,
							(serviceReference, emitter) -> {
								ServletContext servletContext =
									_bundleContext.getService(serviceReference);

								try {
									emitter.emit(
										servletContext.getContextPath());
								}
								finally {
									_bundleContext.ungetService(
										serviceReference);
								}
							});
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		HashedFilesRegistryImpl.class);

	private static int _startIndex = -1;

	private BundleContext _bundleContext;
	private final Map<String, String> _cdnHostMap = new ConcurrentHashMap<>();
	private final Map<String, String> _jsMap = new ConcurrentHashMap<>();
	private final Map<String, String> _map = new ConcurrentHashMap<>();

	@Reference
	private Portal _portal;

	private volatile ServiceTracker<ServletContext, Map<String, String>>
		_serviceTracker;
	private volatile ServiceTrackerMap<String, ServletContext>
		_serviceTrackerMap;

}