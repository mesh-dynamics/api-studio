import React from 'react'
import { Route } from 'react-router'
import SetupTestConfig from "./SetupTestConfig";

export default [
    <Route exact key="TestConfigSetup" path="/test_config_setup" component={SetupTestConfig} />,
]
