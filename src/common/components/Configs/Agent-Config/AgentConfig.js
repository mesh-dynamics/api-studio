import React, { Component, Fragment } from 'react';
import { connect } from "react-redux";
import { JsonEditor as Editor } from 'jsoneditor-react';
import "./AgentConfig.css";
import { cubeService } from "../../../services";
import _ from "lodash";
import 'jsoneditor-react/es/editor.min.css';
import {
    Dropdown, MenuItem, Row, Col, ButtonGroup, Tab,
    NavItem, Nav, FormGroup, FormControl, ControlLabel
} from 'react-bootstrap';
import Ajv from 'ajv';

const ajv = new Ajv({ allErrors: true, verbose: true });

class AgentConfig extends Component {

    state = {
        facets: [],
        tags: [],
        services: [],
        selectedInstance: "Select Instance",
        selectedTag: "Select a Tag",
        selectedService: "Select service",

        instance: "",
        tag: "",
        service: "",

        configs: [],
        configJson: {},
        newAddedJson: {
            "io": {
                "md": {
                    "service": {
                        "record": "https://demo.dev.cubecorp.io/api",
                        "mock": "https://demo.dev.cubecorp.io/api"
                    },
                    "authtoken": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s",
                    "read": {
                        "timeout": 100000
                    },
                    "connect": {
                        "timeout": 100000,
                        "retries": 3
                    },
                    "intent": "record",
                    "samplerconfig": {
                        "type": "simple",
                        "accuracy": 1000,
                        "rate": 1
                    },
                    "sampler": {
                        "veto": false
                    },
                    "nodeselectionconfig": {
                        "type": "adaptive",
                        "accuracy": 1000,
                        "fieldCategory": "customerAttributes",
                        "attributes": [
                            {
                                "field": "io.md.serviceinstance",
                                "value": "1",
                                "rate": 0
                            },
                            {
                                "field": "io.md.serviceinstance",
                                "value": "2",
                                "rate": 1
                            },
                            {
                                "field": "io.md.serviceinstance",
                                "value": "3",
                                "rate": 0
                            },
                            {
                                "field": "io.md.serviceinstance",
                                "value": "4",
                                "rate": 1
                            },
                            {
                                "field": "io.md.serviceinstance",
                                "value": "other",
                                "rate": 1
                            }
                        ]
                    }
                }
            }
        },
        editedJson: {},
    };

    setEditorRef = instance => {
        if (instance) {
            const { jsonEditor } = instance;
            this.editorRef = jsonEditor;
        } else {
            this.editorRef = null;
        }
    };

    componentDidMount() {
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        const app = urlParameters["app"];
        cubeService.fetchAgentConfigs(app).then(res => {
            this.setState({ facets: res.facets.instance_facets });
            this.setState({ configs: res.configs });
        });
    }

    onInstanceSelect = (eventKey) => {
        this.setState({ selectedInstance: eventKey },
            () => this.updateTagList(this.state.facets, this.state.selectedInstance));
    }

    onTagSelect = (eventKey) => {
        this.setState({ selectedTag: eventKey },
            () => this.updateServiceList(this.state.tags, this.state.selectedTag));
    }

    onServiceSelect = (eventKey) => {
        this.setState({ selectedService: eventKey },
            () => this.updateConfig(this.state.configs, this.state.selectedInstance,
                this.state.selectedTag, this.state.selectedService));
    }

    updateTagList = (facets, instance) => {
        const tagObject = _.find(facets, { val: instance });
        if (_.isEmpty(tagObject)) {
            this.setState({ tags: [] });
        }

        this.setState({ tags: tagObject.tag_facets });
    }
    updateServiceList = (tags, tag) => {
        const serviceObject = _.find(tags, { val: tag });
        if (_.isEmpty(serviceObject)) {
            this.setState({ services: [] });
        }

        this.setState({ services: serviceObject.service_facets });
    }

    updateConfig = (configs, instance, tag, service) => {
        const config = _.find(configs, { service: service, instanceId: instance, tag: tag })
        this.setState({ configJson: config });
        this.setState({ editedJson: JSON.parse(config.configJson.config) });
        this.editorRef && this.editorRef.set(JSON.parse(config.configJson.config));
    }

    handleJsonChange = (editedJson) => {
        this.setState({ editedJson });
    }

    handleAddJson = (newAddedJson) => {
        this.setState({ newAddedJson });
    }

    handleInputChange = (e) => {

        switch (e.target.id) {
            case 'Instance':
                this.setState({ instance: e.target.value });
                break;
            case 'Tag':
                this.setState({ tag: e.target.value });
                break;
            case 'Service':
                this.setState({ service: e.target.value });
                break;
            default:
                console.log(`default  ==== ${e.target.id}`);
        }

    }

    onSave = () => {
        const { configJson, editedJson } = this.state;
        const updatedConfig = {
            ...configJson,
            configJson: {
                ...configJson.configJson,
                config: JSON.stringify(editedJson)
            }
        };
        cubeService.updateAgentConfig(updatedConfig);
    }

    onAddConfig = () => {
        const { configJson, newAddedJson, instance, tag, service } = this.state;
        const addedConfig = {
            ...configJson,
            service: service,
            instanceId: instance,
            tag: tag,
            configJson: {
                ...configJson.configJson,
                config: JSON.stringify(newAddedJson)
            }
        };
        cubeService.updateAgentConfig(addedConfig);
    }

    renderEditMode() {
        const { facets, tags, services, selectedInstance,
            selectedTag, selectedService, editedJson, configJson } = this.state;

        return (
            <Fragment>
                <Row className="margin-bottom-10">
                    <Col xs lg="2">
                        <div><ControlLabel>Instance</ControlLabel></div>
                        <ButtonGroup justified>
                            <Dropdown onSelect={this.onInstanceSelect}>
                                <Dropdown.Toggle variant="success" id="instance">
                                    {selectedInstance}
                                </Dropdown.Toggle>
                                <Dropdown.Menu >
                                    {facets.map((value) => (
                                        <MenuItem eventKey={value.val}>
                                            <span>{value.val}</span>
                                        </MenuItem>
                                    ))}
                                </Dropdown.Menu>
                            </Dropdown>
                        </ButtonGroup>
                    </Col>
                    <Col xs lg="2">
                        <div><ControlLabel>Tag</ControlLabel></div>
                        <ButtonGroup justified>
                            <Dropdown onSelect={this.onTagSelect}>
                                <Dropdown.Toggle variant="success" id="tag">
                                    {selectedTag}
                                </Dropdown.Toggle>
                                <Dropdown.Menu >
                                    {tags.map((value) => (
                                        <MenuItem eventKey={value.val}>
                                            <span>{value.val}</span>
                                        </MenuItem>
                                    ))}
                                </Dropdown.Menu>
                            </Dropdown>
                        </ButtonGroup>
                    </Col>
                    <Col xs lg="2">
                        <div><ControlLabel>Service</ControlLabel></div>
                        <ButtonGroup justified>
                            <Dropdown onSelect={this.onServiceSelect}>
                                <Dropdown.Toggle variant="success" id="service">
                                    {selectedService}
                                </Dropdown.Toggle>
                                <Dropdown.Menu >
                                    {services.map((value) => (
                                        <MenuItem eventKey={value.val}>
                                            <span>{value.val}</span>
                                        </MenuItem>
                                    ))}
                                </Dropdown.Menu>
                            </Dropdown>
                        </ButtonGroup>
                    </Col>
                </Row>
                <Row className="margin-bottom-10">
                    <Col xs lg="6" >
                        {!_.isEmpty(editedJson) &&
                            <Editor
                                ref={this.setEditorRef}
                                value={editedJson}
                                onChange={this.handleJsonChange}
                                ajv={ajv}
                                allowedModes={["tree", "code", "view", "form"]}
                                mode='code'
                            />
                        }
                    </Col>
                </Row>
                <Row>
                    <Col xs lg="2">
                        <div className={_.isEmpty(editedJson) ? "cube-btn text-center disabled" : "cube-btn text-center"}
                            style={{ width: "100px" }}
                            onClick={this.onSave}
                        >
                            SAVE
                        </div>
                    </Col>
                </Row>
            </Fragment>
        );
    }

    renderAddMode() {
        const { newAddedJson } = this.state;
        return (
            <Fragment>
                <Row>
                    <form>
                        <Col xs lg="2">
                            <FormGroup
                                controlId="Instance"
                            >
                                <ControlLabel>Instance</ControlLabel>
                                <FormControl
                                    type="text"
                                    placeholder="Enter Instance"
                                    onChange={this.handleInputChange}
                                />
                            </FormGroup>
                        </Col>
                        <Col xs lg="2">
                            <FormGroup
                                controlId="Tag"
                            >
                                <ControlLabel>Tag</ControlLabel>
                                <FormControl
                                    type="text"
                                    placeholder="Enter Tag"
                                    onChange={this.handleInputChange}
                                />
                            </FormGroup>
                        </Col>
                        <Col xs lg="2">
                            <FormGroup
                                controlId="Service"
                            >
                                <ControlLabel>Service</ControlLabel>
                                <FormControl
                                    type="text"
                                    placeholder="Enter Service"
                                    onChange={this.handleInputChange}
                                />
                            </FormGroup>
                        </Col>
                    </form>
                </Row>
                <Row className="margin-bottom-10">
                    <Col xs lg="6">
                        <Editor
                            value={newAddedJson}
                            onChange={this.handleAddJson}
                            ajv={ajv}
                            allowedModes={["tree", "code", "view", "form"]}
                            mode='code'
                        />
                    </Col>
                </Row>
                <Row>
                    <Col xs lg="2">
                        <div className="cube-btn text-center"
                            style={{ width: "100px" }}
                            onClick={this.onAddConfig}
                        >
                            ADD
                        </div>
                    </Col>
                </Row>

            </Fragment>
        );
    }

    render() {

        return (
            <Fragment>
                <Tab.Container id="config" defaultActiveKey="1">
                    <Row className="clearfix">
                        <Nav bsStyle="pills">
                            <NavItem eventKey="1" >View/Edit</NavItem>
                            <NavItem eventKey="2">Add</NavItem>
                        </Nav>
                        <Tab.Content animation style={{ "marginLeft": "5px" }}>
                            <Tab.Pane eventKey="1">{this.renderEditMode()}</Tab.Pane>
                            <Tab.Pane eventKey="2">{this.renderAddMode()}</Tab.Pane>
                        </Tab.Content>
                    </Row>
                </Tab.Container>
            </Fragment>
        )
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

export default connect(mapStateToProps)(AgentConfig);
