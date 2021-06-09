import React from "react";
/**
 * Use this component, when an external link needs to be embedded in Page. This will open out of Devtool
 */
export interface IExternalLinkProps {
  link: string;
  children: React.ReactChild[] | React.ReactChild;
}

export function ExternalLink(props: IExternalLinkProps) {
  var handleClick = React.useCallback((event) => {
    if (PLATFORM_ELECTRON) {
      event.preventDefault();
      window.require("electron").shell.openExternal(props.link);
    }
  }, []);
  return (
    <a href={props.link} target="_blank" title="" onClick={handleClick}>
      {props.children}
    </a>
  );
}
