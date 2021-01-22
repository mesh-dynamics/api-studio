import React, { Component } from 'react';
import { Tabs, Tab } from 'react-bootstrap';
import AgentConfig from "../../components/Configs/Agent-Config/AgentConfig";
import TestConfig from "../../components/Configs/Test-Config/TestConfig";
import ContextPropagationRules from '../../components/Configs/ContextPropagationRules/ContextPropagationRules';
import GrpcConfiguration from '../../components/Configs/Grpc/GrpcConfiguration';

class Config extends Component {

    render() {
        return (
            <div className="content-wrapper">
                <div>
                    <h4 className="inline-block margin-right-10">Configurations</h4>
                </div>
                <Tabs id="controlled-mode">
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
                </Tabs>
            </div>
        );
    }
}

export default Config;
