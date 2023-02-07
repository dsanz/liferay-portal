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

import ClayTabs from '@clayui/tabs';
import React, {useEffect} from 'react';

export default function Tabs({
	activation,
	additionalProps: _additionalProps,
	componentId: _componentId,
	cssClass,
	displayType,
	fade,
	justified,
	locale: _locale,
	panels,
	portletId: _portletId,
	portletNamespace: _portletNamespace,
	tabsItems,
	...otherProps
}) {

	// TO REMOVE ONCE CLAY IS RELEASED

	const activeIndex = tabsItems.indexOf(
		tabsItems.find((item) => item.active)
	);

	function isRenderNode(element) {
	  if (element.nodeName == "DIV" && element.innerText == "") {
        const id = element.getAttribute("id");
        if (id && id.startsWith("__RENDER__")) {
          return id;
      }
    }
	  return null;
  }


	useEffect(() => {
    panels.map((panel) => {
        const panelDocument = new DOMParser().parseFromString(panel, "text/html");
        panelDocument.body.querySelectorAll("div div[id^=__RENDER__]").forEach((c) => {
          const id = isRenderNode(c);
          id && Liferay.fire(id)
        })
      })
    })

	return (
		<>
			<ClayTabs
				activation={activation}
				className={cssClass}
				defaultActive={activeIndex}
				displayType={displayType}
				fade={fade}
				justified={justified}
				{...otherProps}
			>
				<ClayTabs.List>
					{tabsItems.map(
						({/* active,*/ disabled, href, label}, i) => (
							<ClayTabs.Item

								// active={active} REMOVE COMMENT ONCE CLAY IS RELEASED

								disabled={disabled}
								href={href}
								key={i}
							>
								{label}
							</ClayTabs.Item>
						)
					)}
				</ClayTabs.List>

				<ClayTabs.Panels>
					{panels.map((panel, i) => (
						<ClayTabs.TabPanel
							dangerouslySetInnerHTML={{__html: panel}}
							key={i}
						/>
					))}
				</ClayTabs.Panels>
			</ClayTabs>
		</>
	);
}
