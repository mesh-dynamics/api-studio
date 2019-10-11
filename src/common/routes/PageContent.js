import React, { Component } from 'react'
import replay from './replay'
import TestResults from "./test_results";
import PathResults from "./path_results";
import DiffResults from "./diff_results";
import TestConfig from "./test_config";
import SetupTestConfig from "./setup_test_config";
import ViewTestConfig from "./view_test_config";
import ReviewGolden from "./review_golden";
import ShareableLink from "./shareable_link";
import ShareableDiff from "./shareable_diff";


class PageContent extends Component {
  constructor(props) {
    super(props)
    this.state = {
      fullScreenMode: false,
    };
    this.toggleFullScreen = this.toggleFullScreen.bind(this);
  }

  toggleFullScreen() {
    const { fullScreenMode } = this.state;
    this.setState({ fullScreenMode: !fullScreenMode });
  }

  render() {
    const { needMargin } = this.props;
    const { fullScreenMode } = this.state;
    return (
      <div role="main" className={fullScreenMode ? 'main fullscreen' : 'main'}>
        <div className="utility">
          <i onClick={this.toggleFullScreen} className={!fullScreenMode ? "fas fa-expand pull-right link" : "hidden"}></i>
          <i onClick={this.toggleFullScreen} className={fullScreenMode ? "fas fa-compress pull-right link" : "hidden"}></i>
        </div>
        {TestResults}
        {PathResults}
        {DiffResults}
        {ReviewGolden}
        {replay}
        {TestConfig}
        {SetupTestConfig}
        {ViewTestConfig}
        {ShareableLink}
        {ShareableDiff}
      </div>
    )
  }
}

export default PageContent
