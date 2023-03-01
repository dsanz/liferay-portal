/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */


package com.liferay.frontend.data.set.views.web.internal.fragment.renderer;

import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererContext;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.frontend.js.loader.modules.extender.npm.ModuleNameUtil;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;
import com.liferay.frontend.taglib.clay.internal.js.loader.modules.extender.npm.NPMResolverProvider;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.servlet.taglib.util.OutputData;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.template.react.renderer.ComponentDescriptor;
import com.liferay.portal.template.react.renderer.ReactRenderer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Daniel Sanz
 */

@Component(service = FragmentRenderer.class)
public class FDSEntryFragmentRenderer implements FragmentRenderer {
  @Override
  public String getCollectionKey() {
    return "content-display";
  }

  @Override
  public String getConfiguration(
    FragmentRendererContext fragmentRendererContext) {

    return JSONUtil.put(
      "fieldSets",
      JSONUtil.putAll(
        JSONUtil.put(
          "fields",
          JSONUtil.putAll(
            JSONUtil.put(
              "label", "dataset"
            ).put(
              "name", "itemSelector"
            ).put(
              "type", "itemSelector"
            ).put(
              "typeOptions",
              JSONUtil.put("itemType",
                _FDSEntryObjectDefinition.getClassName())
            ))))).toString();
  }

  @Override
  public String getIcon() {
    return "web-content";
  }

  @Override
  public boolean isSelectable(
    HttpServletRequest httpServletRequest) {
    return false;
  }

  @Override
  public void render(
    FragmentRendererContext fragmentRendererContext,
    HttpServletRequest httpServletRequest,
    HttpServletResponse httpServletResponse) throws IOException {

    try {
      PrintWriter printWriter = httpServletResponse.getWriter();

      FragmentEntryLink fragmentEntryLink =
      			fragmentRendererContext.getFragmentEntryLink();

      JSONObject configurationJSONObject =
      	_jsonFactory.createJSONObject();

      			if (Validator.isNotNull(fragmentEntryLink.getConfiguration())) {
              configurationJSONObject =
                _fragmentEntryConfigurationParser.
                  getConfigurationJSONObject(
                    fragmentEntryLink.getConfiguration(),
                    fragmentEntryLink.getEditableValues(),
                    LocaleUtil.getMostRelevantLocale());
            }


      printWriter.write(
        _renderFragmentEntry(
          fragmentRendererContext.getFragmentElementId(),
          fragmentRendererContext,
          configurationJSONObject,
          httpServletRequest));
    }
    catch (PortalException portalException) {
      throw new IOException(portalException);
    }
  }

  private String _renderFragmentEntry(
    String fragmentElementId,
    FragmentRendererContext fragmentRendererContext,
    JSONObject configurationJSONObject, HttpServletRequest httpServletRequest)
    throws IOException {

    NPMResolver npmResolver = NPMResolverProvider.getNPMResolver();

    String moduleName = npmResolver.resolveModuleName(
  		"@liferay/frontend-data-set-web/FrontendDataSet");

    StringBundler sb = new StringBundler(9);

    sb.append("<div id=\"");
    sb.append(fragmentElementId);
    sb.append("\" >");

    Writer writer = new CharArrayWriter();

    ComponentDescriptor componentDescriptor = new ComponentDescriptor(
  			moduleName, fragmentElementId);

    _reactRenderer.renderReact(
      componentDescriptor, prepareData(configurationJSONObject)
      , httpServletRequest, writer);

    sb.append(writer.toString());

    sb.append("</div>");

    return sb.toString();

    }

    private Map<String, Object> prepareData(JSONObject configurationJSONObject) {
      return HashMapBuilder.<String, Object>put(
      			"customViews", ""
      		).put(
      			"namespace", getNamespace()
      		).put(
      			"pagination",
      			HashMapBuilder.<String, Object>put(
      				"deltas", _fdsPaginationEntries
      			).put(
      				"initialDelta", 10
      			).put(
      				"initialPageNumber", 1
      			).build()
      		).put(
      			"selectedItems", ""
      		).put(
      			"uniformActionsDisplay", false
      		).build();
    }

    @Reference
   	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;


   	@Reference
   	private JSONFactory _jsonFactory;

  @Reference
 	private ReactRenderer _reactRenderer;
}

