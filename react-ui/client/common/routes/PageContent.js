import React, { Component } from 'react'
import config from './config'
import replay from './replay'
import analysis from "./analysis";


class PageContent extends Component {
  render() {
    const { needMargin } = this.props;
    return (
      <div className="padding-15" role="main">
        { replay }
        { config }
        { analysis }
      </div>
    )
  }
}

export default PageContent
