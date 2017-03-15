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

package com.liferay.dynamic.data.mapping.data.provider;

/**
 * @author Leonardo Barros
 */
public class DDMDataProviderRequest {

	public DDMDataProviderRequest(
		DDMDataProviderContext ddmDataProviderContext) {

		_ddmDataProviderContext = ddmDataProviderContext;
	}

	public DDMDataProviderContext getDDMDataProviderContext() {
		return _ddmDataProviderContext;
	}

	public int getPaginationEnd() {
		return _paginationEnd;
	}

	public int getPaginationStart() {
		return _paginationStart;
	}

	public void setPaginationEnd(int paginationEnd) {
		_paginationEnd = paginationEnd;
	}

	public void setPaginationStart(int paginationStart) {
		_paginationStart = paginationStart;
	}

	private final DDMDataProviderContext _ddmDataProviderContext;
	private int _paginationEnd;
	private int _paginationStart;

}