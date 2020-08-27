import React, { Component } from 'react';
import { Switch } from 'react-router-dom';
import TestResults from "./test_results";
import DiffResults from "./diff_results";
import Configs from "./configs";
import ViewTestConfig from "./view_test_config";
import TestReport from "./test_report";
import ViewTrace from "./view_trace";
import APICatalog from "./api_catalog";
import HttpClientTabs from "./http_client";

class PageContent extends Component {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div role="main" className='main'>
        <Switch>
          {DiffResults}
          {Configs}
          {ViewTrace}
          {TestReport}
          {ViewTestConfig}
          {APICatalog}
          {HttpClientTabs}
          {/* This has to be at the bottom since it has default routing handler */}
          {TestResults}
        </Switch>
      </div>
    )
  }
}

export default PageContent

// import replay from './service_graph'
// import PathResults from "./path_results";
// import SetupTestConfig from "./setup_test_config";
// import ReviewGolden from "./review_golden";
// import ShareableLink from "./shareable_link";
// import ShareableDiff from "./shareable_diff";
// {/* {PathResults} */}
//         {/* {ReviewGolden} */}
//         {/* {SetupTestConfig} */}
//         {/* {ShareableLink} */}
//         {/* {ShareableDiff} */}
//         {/*  */}

// {replay}
