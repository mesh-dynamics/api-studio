import React, { Component } from 'react'
import config from './config'
import replay from './replay'


class PageContent extends Component {
  render() {
    const { needMargin } = this.props;
    return (
      <div className={ needMargin ? "right_col" : "" } role="main">
        { config }
        { replay }
      </div>
    )
  }
}

export default PageContent
