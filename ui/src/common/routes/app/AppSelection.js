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

import React, { Component } from 'react';
import { connect } from 'react-redux';

class AppSelection extends Component {
    constructor(props) {
        super(props);
    }

    render() {

    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}



const connectedAppSelection = connect(mapStateToProps)(AppSelection);
export default connectedAppSelection