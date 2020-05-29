import React from 'react'
import { Route } from 'react-router'
import HttpClient from "./HttpClient";

export default [
    <Route exact key="HttpClient" path="/http_client" component={HttpClient} />,
]
