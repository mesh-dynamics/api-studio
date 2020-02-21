import React, { Component } from 'react'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import {cubeActions} from "../../actions";

export default class DiffResultsFilter extends Component {

    constructor(props) {
        super(props);
        this.state = {
            app: "test",
            currentPageNumber: 1,
        }
        // TODO: props
        this.services = [{value: "s1", count: 2}, {value: "s2", count: 2}];
        this.selectedService = "s1";

        this.apiPaths = [{value: "s1", count: 2}, {value: "s2", count: 2}];
        this.selectedAPI = "s1";

        this.selectedReqRespMatchType = "responseMismatch";

        this.pages = 10;
        //this.currentPageNumber = 1;
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

    renderPageButtons = () => {
        let pageButtons = [];
        for(let idx = 1; idx <= this.pages; idx++) {
            pageButtons.push(
                <Button onClick={this.changePageNumber} bsStyle={this.state.currentPageNumber === idx ? "primary" : "default"} style={{}}>{idx}</Button>
            );
        }
        return pageButtons;
    }
    
    handleMetaDataSelect = (metaDataType, value) => {
    }

    toggleMessageContents = (e) => {    
    }

    handleReqRespMtChange = (e) => {
    }
    
    changePageNumber = (e) => {
        this.setState({currentPageNumber : +e.target.innerHTML.trim()});
    }

    handleBackToDashboardClick = () => {
        const { history, dispatch } = this.props;
        dispatch(cubeActions.clearPathResultsParams());
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
                <ButtonGroup style={{ marginBottom: "9px", width: "100%" }}>
                    <div style={{ textAlign: "left" }}>{this.renderPageButtons()}</div>
                </ButtonGroup>
            </div>
        )
    }
}
