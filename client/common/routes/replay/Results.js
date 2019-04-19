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
        const {res} = this.props;
        let tabElem = tabs.map(item => (
            <div onClick={() => {this.changeTab(item.value)}} className={currentView == item.value ? 'selected res-tab' : 'res-tab'}>
                {item.name}
            </div>
        ));

        let view;
        switch (this.state.currentView) {
            case "gateway_service":
                view = (<GatewayTab res={res}/>);
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
