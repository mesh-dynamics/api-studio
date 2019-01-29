import React, { Component } from 'react'
import { Link } from 'react-router-dom'
import cn from 'classnames'

class MenuItem extends Component {
  render () {
    const { icon, title, isActive, shared, ancestors, ...rest } = this.props
    const { to } = rest
    const currentPage = isActive(to)
    const { visiblePath } = shared.getState()

    // if !visiblePath, then this is the very first render of 
    // the UI. If you paste a URL, this effectively opens the 
    // left hand side in a declarative fashion. 
    if (currentPage && !visiblePath)
      shared.setVisiblePath(ancestors);
    return (
      
      <li className={cn({'current-page': currentPage})}><Link {...rest}>
      { icon ? <i className={ `fas fa-${icon}` }></i> : '' }
      { " " + title}</Link></li>
    )
  }
}

export default MenuItem