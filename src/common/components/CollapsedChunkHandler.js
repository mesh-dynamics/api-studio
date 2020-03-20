import React, { Component } from 'react';

class CollapsedChunkHandler extends Component {
    constructor(props) {
        super(props);
        this.showMore = this.showMore.bind(this);
    }

    showMore(e) {
        const { jsonPath , recordReqId, replayReqId } = this.props;
        this.props.handleCollapseLength(e, jsonPath, recordReqId, replayReqId);
    }

    render() {
        return (
            <span onClick={this.showMore}>
                <i className="fas fa-compress" style={{fontSize: "16px", cursor: "pointer"}}></i>
            </span>
        );
    }
}

export default CollapsedChunkHandler;