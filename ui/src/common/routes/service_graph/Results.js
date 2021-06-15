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
import ScatterPlot from "../../components/Graph/ScatterPlot";
import {Clearfix} from "react-bootstrap";
import './Results.css';
import GatewayTab from "../../components/Result-Tabs/GatewayTab";
import SupportingTab from "../../components/Result-Tabs/SupportingTab";
import VirtTab from "../../components/Result-Tabs/VirtTab";
import PerformanceTab from "../../components/Result-Tabs/PerformanceTab";

class Results extends Component {
    constructor(props) {
        super(props);
        this.state = {
            currentView: 'gateway_service',
            tabs: [{
                name: 'Gateway Service',
                value: 'gateway_service',
            }, {
                name: 'Virtualization Services',
                value: 'virtualization_services',
            }, {
                name: 'Supporting services',
                value: 'supporting_services',
            }, {
                name: 'Performance',
                value: 'performance',
            }]
        };
    }

    changeTab(val) {
        if (val === 'gateway_service') {
            this.setState({currentView: val});
        }
    }

    render() {
        const {tabs, currentView} = this.state;
        const {res, resByPath, timeline, app} = this.props;
        let tabElem = tabs.map(item => (
            <div onClick={() => {this.changeTab(item.value)}} className={currentView == item.value ? 'selected res-tab' : 'res-tab'}>
                {item.name}
            </div>
        ));

        let view;
        switch (this.state.currentView) {
            case "gateway_service":
                view = (<GatewayTab app={app} res={res} resByPath={resByPath} timeline={timeline}/>);
                break;

            case "supporting_services":
                view = <SupportingTab/>;
                break;

            case "virtualization_services":
                view = <VirtTab/>;
                break;

            case "performance":
                view = <PerformanceTab/>;
                break;
        };

        return (
            <div className="result-wrapper">
                {tabElem}
                <Clearfix />
                {view}
            </div>
        );
    }
}

export default Results;
