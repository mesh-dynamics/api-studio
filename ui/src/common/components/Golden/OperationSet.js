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
import { Glyphicon } from 'react-bootstrap';
import Modal from "react-bootstrap/lib/Modal";
import GoldenPopover from "../GoldenPopover";
import { DiffResultsContext } from "../../routes/diff_results/DiffResults";
import "./OperationalSet.css";

class OperationSet extends Component {
    constructor(props) {
        super(props);
        this.inputElementRef = this.props.inputElementRef;
        this.state = {
            showPopoverTrigger: false,
            showPopover: false  
        };
    }

    handleShowPopoverTrigger = () => this.setState({ showPopoverTrigger: true });

    handleHidePopoverTrigger = () => this.setState({ showPopoverTrigger: false });

    handleShowPopoverClick = () => this.setState({ showPopover: true });

    handleHidePopoverClick = () => this.setState({ showPopover: false });

    filterPath = () => {
        let inputElement = document.getElementById("filterPathInputId");
        inputElement.value = this.props.jsonPath.replace("<BEGIN>", "").replace("<END>", "");
        let event = new Event("change");
        inputElement.dispatchEvent(event);
        this.inputElementRef.current.props.onChange(event);
    }

    renderOperationalSet = (context) => {
        const { popoverCurrentPath, setPopoverCurrentPath } = context;
        const { showPopover, showPopoverTrigger } = this.state;

        return (
            (showPopover || showPopoverTrigger) 
            &&
                <span className="os-actions-container"ref={this.props.elementRef}>
                    <span 
                            className="pointer" 
                            onClick={
                                () => 
                                {
                                        this.handleShowPopoverClick();
                                        setPopoverCurrentPath(this.props.jsonPath);
                                }
                            }
                        >
                            <Glyphicon glyph="plus" />
                    </span>
                    <span className="pointer" onClick={this.filterPath}><Glyphicon glyph="search" /></span>
                </span>
        );
    };


    render() {
        return (
            ((this.props.jsonPath && this.props.jsonPath.indexOf("<END>") < 0) || (this.props.jsonPath?.includes("<BEGIN>") && this.props.jsonPath?.includes("<END>")))
            ? 
                (
                    <div 
                        onMouseEnter={this.handleShowPopoverTrigger} 
                        onMouseLeave={this.handleHidePopoverTrigger} 
                        className="os-root-container"
                    >
                        <DiffResultsContext.Consumer>
                                {(context) => this.renderOperationalSet(context)}
                        </DiffResultsContext.Consumer>
                        <Modal show={this.state.showPopover} dialogClassName="os-popover-modal popover-golden">
                            <div 
                                className="os-popover-wrapper grey" 
                                id={`tooltip-${this.props.jsonPath}`}
                            >
                                <GoldenPopover 
                                    {...this.props} 
                                    handleHidePopoverClick={this.handleHidePopoverClick} 
                                />
                            </div>
                        </Modal>
                    </div>
                )
            : "");
    }
}

export default OperationSet;

	// import { ShareableLinkContext } from "../../routes/shareable_link/ShareableLink";
    /* TODO: temporary workaround; cleanup when removing shareable_link page */	
    // ((window.location.pathname.includes("/diff_results")) && 	
    
    // || 	
    // (<ShareableLinkContext.Consumer>
    //     {(context) => this.renderOperationalSet(context)}
    // </ShareableLinkContext.Consumer>)
