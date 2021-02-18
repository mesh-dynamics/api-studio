import React, { Component } from 'react';
import { Tabs, Tab } from 'react-bootstrap';
import AgentConfig from "../../components/Configs/Agent-Config/AgentConfig";
import TestConfig from "../../components/Configs/Test-Config/TestConfig";
import ContextPropagationRules from '../../components/Configs/ContextPropagationRules/ContextPropagationRules';
import GrpcConfiguration from '../../components/Configs/Grpc/GrpcConfiguration';
import ApiToken from '../../components/Configs/ApiToken/ApiToken';
import * as URL from "url";
import ComparisonRules from '../../components/Configs/ComparisonRules/ComparisonRules';
export interface IConfigSettingsState{
    selectedTabKey: number
}
class Config extends Component<any, IConfigSettingsState> {
    constructor(props:any){
        super(props);
        this.state = {
            selectedTabKey: 1
        }
    }
    componentDidMount(){
        const parsedUrlObj = URL.parse(window.location.href, true);
        const tabId = parseInt(parsedUrlObj.query.tabId?.toString() || "1");
        this.setState({selectedTabKey: tabId});
    }
    handleSelectedTabChange = (changedKey: any) => {
        this.setState({selectedTabKey: changedKey});
    }

    render() {
        return (
            <div className="content-wrapper">
                <div>
                    <h4 className="inline-block margin-right-10">Configurations</h4>
                </div>
                <Tabs id="controlled-mode" onSelect={this.handleSelectedTabChange} activeKey={this.state.selectedTabKey}>
                    <Tab eventKey={1} title="Test Configurations">
                        <div className="margin-top-20">
                            <TestConfig />
                        </div>

                    </Tab>
                    <Tab eventKey={2} title="Agent Configurations">
                        <div className="margin-top-20">
                            <AgentConfig />
                        </div>
                    </Tab>
                    <Tab eventKey={3} title="Context Propagation Rules">
                        <div className="margin-top-20">
                            <ContextPropagationRules />
                        </div>
                    </Tab>
                    <Tab eventKey={4} title="gRPC">
                        <div className="margin-top-20">
                            <GrpcConfiguration />
                        </div>
                    </Tab>
                    <Tab eventKey={5} title="API Token">
                        <div className="margin-top-20">
                            <ApiToken />
                        </div>
                    </Tab>
                    <Tab eventKey={6} title="Comparison Rules">
                        {this.state.selectedTabKey == 6 && 
                        <div className="margin-top-20">
                            <ComparisonRules />
                        </div>
                        }
                    </Tab>
                </Tabs>
            </div>
        );
    }
}

export default Config;
