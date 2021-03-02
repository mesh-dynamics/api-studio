import React, { Component } from 'react';
import { Switch, Route, Redirect } from 'react-router-dom';
import { TestResults } from "./test_results";
import { DiffResults } from "./diff_results";
import { Configs } from "./configs";
import { ViewTestConfig } from "./view_test_config";
import { TestReport } from "./test_report";
import { ViewTrace } from "./view_trace";
import { APICatalog } from "./api_catalog";
import { HttpClientTabs } from "./http_client";
import { Account } from './account';
import GettingStarted from './GettingStarted';

class PageContent extends Component {
  constructor(props) {
    super(props)
  }

  getGettingStartedScreen(){
    return <GettingStarted />
  }

  getPlatformSpecificRoutes() {
    if (PLATFORM_ELECTRON) {
      return [<Route key="RootRedirect" path="/*"><Redirect to="/http_client" /></Route>]
    } else {
      return [<Route key="RootRedirect" path="/*"><Redirect to="/test_results" /></Route>]
    }
  }

  render() {
    return (
      <div role="main" className='main'>
        <Switch>
          <Route exact key="DiffResults" path="/diff_results" component={DiffResults} />
          <Route exact key="Configs" path="/configs" component={Configs} />
          <Route exact key="ViewTrace" path="/view_trace" component={ViewTrace} />
          <Route exact key="TestReport" path="/test_report" component={TestReport} />
          <Route key="TestConfigView" path="/test_config_view" component={ViewTestConfig} />
          <Route exact key="TestResults" path="/test_results" component={TestResults} />
          <Route key="APICatalog" path="/api_catalog" component={APICatalog} />
          <Route exact key="HttpClientTabs" path="/http_client" component={HttpClientTabs} />
          <Route path="/account" component={Account}></Route>
          {/* This has to be at the bottom since it has default routing handler */}
          {this.getPlatformSpecificRoutes()}
        </Switch>
        {this.getGettingStartedScreen()}
      </div>
    )
  }
}

export default PageContent