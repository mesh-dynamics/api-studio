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

class Breadcrumb extends Component {
    render() {
        const { crumbs } = this.props;
        let jsxContent = crumbs.map(item => {
            return (
                <div className="crumb-unit">
                    <div className="label-n">{item.label}</div>
                    <div className="value-n">{item.value}</div>
                </div>
            );
        });

        return <div className="margin-bottom-10">{jsxContent}</div>;
    }
}

export default Breadcrumb;
