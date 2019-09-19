import React from 'react'
import { Glyphicon } from 'react-bootstrap'
import Tippy from '@tippy.js/react'
import 'tippy.js/themes/light.css'
import _ from 'lodash';
import GoldenPopover from "../GoldenPopover";

class OperationSet extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.handleShow = this.handleShow.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.handleClick = this.handleClick.bind(this);
        this.handleTippyHide = this.handleTippyHide.bind(this);
        this.handleTippyShow = this.handleTippyShow.bind(this);
        this.state = {
            show: false,
            popover: false,
        };
        this.inputElementRef = this.props.inputElementRef;
        this.filterPath = this.filterPath.bind(this);
    }

    handleClose() {
        this.setState({ show: false });
    }

    handleShow() {
        this.setState({ show: true });
    }

    handleClick() {
        this.setState({ show: true });
    }

    handleTippyHide() {
        this.setState({popover: false});
    }

    handleTippyShow() {
        this.setState({popover: true});
    }

    filterPath() {
        let inputElement = document.getElementById("filterPathInputId");
        inputElement.value = this.props.jsonPath.replace("<BEGIN>", "");
        let event = new Event("change");
        inputElement.dispatchEvent(event);
        this.inputElementRef.current.props.onChange(event);
    }


    render() {
        let props = this.props;
        return this.props.jsonPath && this.props.jsonPath.indexOf("<END>") < 0 ? (
            <div onMouseOver={this.handleShow} onMouseOut={this.handleClose} onClick={this.handleClick}>
                <span ref={this.props.elementRef} style={{ visibility: this.state.show || this.state.popover ? "visible" : "hidden" }} >
                <Tippy arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true} onHide={this.handleTippyHide} onShow={this.handleTippyShow} maxWidth={700}
                content={
                        <div style={{padding: 0, width: "auto", maxWidth: "700px", maxHeight:"500px", fontSize: "14px"}} className="grey" id={`tooltip-${this.props.jsonPath}`}>
                            <GoldenPopover {...props} />
                        </div>
                    }>
                        <span style={{ paddingRight: "3px", cursor: "pointer" }}><Glyphicon glyph="plus" /></span>
                    </Tippy>
                    <span style={{cursor: "pointer"}} onClick={this.filterPath}><Glyphicon glyph="search" /></span>
                </span>
            </div>
        ) : "";
    }
}

export default OperationSet;