import React from 'react'
import { Route } from 'react-router'
import Dashboard from './dashboard'
import HomeMenu from './Menu'

export default <Route path="/" exact component={Dashboard} />
export { HomeMenu }
