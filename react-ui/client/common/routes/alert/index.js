import React from 'react'
import { Route } from 'react-router'
import AlertMenu from './Menu'
import AlertsSample from './alertsSample'

export default [
  <Route key="alertsSample" path="/alerts/sysalerts" component={AlertsSample} />,
]
export { AlertMenu }