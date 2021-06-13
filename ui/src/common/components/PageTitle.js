/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { Col, Button, FormControl } from 'react-bootstrap'
import InputController from './InputController'

const propTypes = {
  title: PropTypes.string,
  smallTitle: PropTypes.string,
  hideSearch: PropTypes.bool,
  onSearch: PropTypes.func
}

const defaultProps = {
  showSearch: false,
  onSearch: () => { }
}

class PageTitle extends Component {
  constructor(props) {
    super(props)
    this.searchInputController = InputController(this, 'search', '')
  }

  render () {
    const { title, smallTitle, showSearch, onSearch } = this.props

    return (
      <div className="page-title">
        <div className="title_left">
          <h3> {title} <small> {smallTitle}</small> </h3>
        </div>
        { showSearch ? <Search onSearch={onSearch} controller={this.searchInputController()} /> : null }
      </div>
    )
  }
}


function Search ({onSearch, controller}) {
  const { value } = controller
  const onSubmit = e => {
    e.preventDefault()
    onSearch(value, e)
  }

  return (
    <div className="title_right">
      <Col md={5} sm={5} xs={12} className="form-group pull-right top_search">
        <form className="input-group" onSubmit={onSubmit}>
          <FormControl { ...controller } placeholder="Search for..." />
          <span className="input-group-btn">
              <Button onClick={e => onSearch(value, e)}>Go!</Button>
          </span>
        </form>
      </Col>
    </div>    
  )
}

PageTitle.propTypes = propTypes
PageTitle.defaultProps = defaultProps

export default PageTitle