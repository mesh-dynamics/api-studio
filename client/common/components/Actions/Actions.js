import React from 'react'
import { Dropdown } from 'react-bootstrap'
import { MenuItem } from 'react-bootstrap'
import { Glyphicon } from 'react-bootstrap'
import { Modal } from 'react-bootstrap'

import EditTemplate from './EditTemplate'
import FileReport from './FileReport'
import PostComment from './PostComment'
import UpdateGolden from './UpdateGolden'

class Actions extends React.Component {
    constructor(props, context) {
        super(props, context);

        this.handleShow = this.handleShow.bind(this);
        this.handleClose = this.handleClose.bind(this);

        this.state = {
            show: false,
            editTemplate: false,
            fileReport: false,
            postComment: false,
            updateGolden: false,
            modalTitle: ''
        };
    }

    handleClose() {
        this.setState({ show: false });
    }

    handleShow(form, modalTitle) {
        this.setState({ show: true });
        this.setState({ editTemplate: false });
        this.setState({ fileReport: false });
        this.setState({ postComment: false });
        this.setState({ updateGolden: false });
        this.setState({ [form]: true });
        this.setState({ modalTitle: modalTitle });
    }

    render() {
        const element = (
            <div ref={this.props.actionsWrapperElementRef} style={{ visibility: "hidden", margin: "5px" }}>
                <Dropdown pullRight={true} className="">
                    <Dropdown.Toggle className="btn-sm btn-primary">
                        Actions
                    </Dropdown.Toggle>
                    <Dropdown.Menu className="pull-right">
                        <MenuItem eventKey="1" onClick={() => this.handleShow("editTemplate", "Edit Template")}>
                            <Glyphicon glyph="edit" /> 
                            <span style={{position: "relative", bottom: "1px", paddingLeft: "7px"}}>Edit Template</span>
                        </MenuItem>
                        <MenuItem eventKey="2" onClick={() => this.handleShow("fileReport", "File Report")}>
                            <Glyphicon glyph="tag" /> 
                            <span style={{position: "relative", bottom: "1px", paddingLeft: "7px"}}>File Report</span>
                        </MenuItem>
                        <MenuItem eventKey="3" onClick={() => this.handleShow("postComment", "Post Comment")}>
                            <Glyphicon glyph="send" /> 
                            <span style={{position: "relative", bottom: "1px", paddingLeft: "7px"}}>Post Comment</span>
                        </MenuItem>
                        <MenuItem eventKey="4" onClick={() => this.handleShow("updateGolden", "Update Golden")}>
                            <Glyphicon glyph="cloud-upload" /> 
                            <span style={{position: "relative", bottom: "1px", paddingLeft: "7px"}}>Update Golden</span>
                        </MenuItem>
                    </Dropdown.Menu>
                </Dropdown>

                <Modal show={this.state.show} onHide={this.handleClose}>
                    <Modal.Header closeButton>
                        <Modal.Title>{this.state.modalTitle}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.state.editTemplate && (<EditTemplate hideModal={this.handleClose} />)}
                        {this.state.fileReport && (<FileReport hideModal={this.handleClose} />)}
                        {this.state.postComment && (<PostComment hideModal={this.handleClose} />)}
                        {this.state.updateGolden && (<UpdateGolden hideModal={this.handleClose} />)}
                    </Modal.Body>
                </Modal>
            </div>
        );
        return this.props.jsonPath.indexOf("<END>") < 0 ? element : "";
    }
}

export default Actions;