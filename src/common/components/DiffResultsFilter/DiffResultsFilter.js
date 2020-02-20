import React, { Component } from 'react'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';

export default class DiffResultsFilter extends Component {

    constructor(props) {
        super(props);
        this.state = {
            app: "test",
        }
        // TODO: props
        this.services = [{value: "s1", count: 2}, {value: "s2", count: 2}];
        this.selectedService = "s1";

        this.apiPaths = [{value: "s1", count: 2}, {value: "s2", count: 2}];
        this.selectedAPI = "s1";

        this.selectedReqRespMatchType = "responseMismatch";
    }

    serviceMenuItems = () => {
        //const { services } = this.props;
        //const { selectedService } = this.props;
        const services = this.services; // todo: props
        const selectedService = this.selectedService; // TODO
        return services.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedService", item.value)}>
                <Glyphicon style={{ visibility: selectedService === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
    }

    apiPathMenuItems = () => {
        //const { apiPaths } = this.props;
        //const { selectedAPI } = this.props;
        const apiPaths = this.apiPaths; // todo: props
        const selectedAPI = this.selectedAPI; // TODO
        return apiPaths.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedAPI", item.value)}>
                <Glyphicon style={{ visibility: selectedAPI === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
    }
    
    handleMetaDataSelect = (metaDataType, value) => {
    }

    toggleMessageContents = (e) => {    
    }

    handleReqRespMtChange = (e) => {
    }
    
    render() {
        let selectedService = this.selectedService;
        let selectedAPI = this.selectedAPI;
        let apiPaths = this.apiPaths;

        return (
            <div>
            <Breadcrumb style={{}}>
                <Breadcrumb.Item href="/">{this.state.app}</Breadcrumb.Item>

                <Breadcrumb.Item href="javascript:void(0);">
                    <strong>Service:&nbsp;</strong>
                    <DropdownButton title={selectedService} id="dropdown-size-medium">
                        <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedService", "All")}>
                            <Glyphicon style={{ visibility: selectedService === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({this.services.reduce((accumulator, item) => accumulator += item.count, 0)})
                        </MenuItem>
                        <MenuItem divider />
                        {this.serviceMenuItems()}
                    </DropdownButton>
                </Breadcrumb.Item>

                <Breadcrumb.Item active>
                    <strong>API Path:&nbsp;</strong>
                    <DropdownButton title={selectedAPI ? selectedAPI : "Select API Path"} id="dropdown-size-medium">
                        <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedAPI", "All")}>
                            <Glyphicon style={{ visibility: selectedAPI === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({apiPaths.reduce((accumulator, item) => accumulator += item.count, 0)})
                        </MenuItem>
                        <MenuItem divider />
                        {this.apiPathMenuItems()}
                    </DropdownButton>
                </Breadcrumb.Item>
            </Breadcrumb>

            <div style={{ marginBottom: "18px" }}>
                <Radio inline value="responseMismatch" checked={this.selectedReqRespMatchType === "responseMismatch"} onChange={this.handleReqRespMtChange}> Response Mismatches only </Radio>
                <Radio inline value="requestMismatch" checked={this.selectedReqRespMatchType === "requestMismatch"} onChange={this.handleReqRespMtChange}> Request Mismatches only </Radio>
                <Radio inline value="All" checked={this.selectedReqRespMatchType === "All"} onChange={this.handleReqRespMtChange}> All </Radio>
            </div>            
        </div>
        )
    }
}
