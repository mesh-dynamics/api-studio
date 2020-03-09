import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import Popover, { ArrowContainer } from 'react-tiny-popover';
import GoldenPopover from "../GoldenPopover";
import { ShareableLinkContext } from "../../routes/shareable_link/ShareableLink";
import { DiffResultsContext } from "../../routes/diff_results/DiffResults";
import "./OperationalSet.css";
import {history} from "../../helpers/history.js"

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
        inputElement.value = this.props.jsonPath.replace("<BEGIN>", "");
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
                    <Popover
                        isOpen={showPopover && popoverCurrentPath === this.props.jsonPath}
                        position={['top', 'bottom', 'left', 'right']}
                        padding={10}
                        containerStyle={{ zIndex: 100 }}
                        content={({ position, targetRect, popoverRect }) => (
                            <ArrowContainer
                                position={position}
                                targetRect={targetRect}
                                popoverRect={popoverRect}
                                arrowColor={'grey'}
                                arrowSize={10}
                                arrowStyle={{ opacity: 0.7 }}
                            >
                                <div 
                                    className="os-popover-wrapper grey" 
                                    id={`tooltip-${this.props.jsonPath}`}
                                >
                                    <GoldenPopover 
                                        {...this.props} 
                                        handleHidePopoverClick={this.handleHidePopoverClick} 
                                    />
                                </div>
                            </ArrowContainer>
                        )}
                    >
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
                    </Popover>
                    <span className="pointer" onClick={this.filterPath}><Glyphicon glyph="search" /></span>
                </span>
        );
    };


    render() {
        return (
            (this.props.jsonPath && this.props.jsonPath.indexOf("<END>") < 0)
            ? 
                (
                    <div 
                        onMouseEnter={this.handleShowPopoverTrigger} 
                        onMouseLeave={this.handleHidePopoverTrigger} 
                        onClick={this.handlePopoverTriggerClick}
                        className="os-root-container"
                    >
                        {
                            /* TODO: temporary workaround; cleanup when removing shareable_link page */
                        (history.location.pathname.includes("/diff_results")) && 
                        <DiffResultsContext.Consumer>
                            {(context) => this.renderOperationalSet(context)}
                        </DiffResultsContext.Consumer> 
                        || 
                        <ShareableLinkContext.Consumer>
                            {(context) => this.renderOperationalSet(context)}
                        </ShareableLinkContext.Consumer>
                    }
                    </div>
                )
            : "");
    }
}

export default OperationSet;