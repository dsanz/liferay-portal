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

package com.liferay.sync.engine.lan.session;

import com.liferay.sync.engine.lan.util.LanClientUtil;

import java.io.IOException;

import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

/**
 * @author Dennis Ju
 */
public class SNISSLConnectionSocketFactory extends SSLConnectionSocketFactory {

	public SNISSLConnectionSocketFactory(
		SSLContext sslContext, HostnameVerifier hostnameVerifier) {

		super(sslContext, hostnameVerifier);
	}

	@Override
	public Socket createLayeredSocket(
			Socket socket, String target, int port, HttpContext httpContext)
		throws IOException {

		HttpClientContext httpClientContext = (HttpClientContext)httpContext;

		HttpRequest httpRequest = httpClientContext.getRequest();

		RequestLine requestLine = httpRequest.getRequestLine();

		String[] parts = StringUtils.split(requestLine.getUri(), "/");

		String sniCompliantLanServerUuid = LanClientUtil.getSNIHostname(
			parts[0]);

		return super.createLayeredSocket(
			socket, sniCompliantLanServerUuid, port, httpContext);
	}

}