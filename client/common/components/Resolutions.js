import React from 'react'
import {Tooltip} from 'react-bootstrap'
import {OverlayTrigger} from 'react-bootstrap'
import {Glyphicon} from 'react-bootstrap'


const resolutionsIconMap = {
    "OK": {
        "description": "OK:exact match",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "OK_Optional": {
        "description": "OK:Optional (item is missing in the test, and the item is optional)",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "OK_Ignore": {
        "description": "OK:Ignore (instruction to ignore comparison)",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "OK_CustomMatch": {
        "description": "OK:CustomMatch (matched after applying custom matching)",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "OK_OtherValInvalid": {
        "description": "OK:Invalid, // the val to compare against does not exist or is not of right type in the master",
        "icon": "warning-sign",
        "color": "#ffdd4b"
    },
    "OK_OptionalMismatch": {
        "description": "OK:OptionalMismatch, // vals mismatched but comparison type was EqualOptional (used in scoring case for prioritizing) [This should never happen in the comparison]",
        "icon": "warning-sign",
        "color": "#ffdd4b"
    },
    "ERR_NotExpected": {
        "description": "ERROR: Not expected",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR_Required": {
        "description": "ERROR:Required (required item missing in test)",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR_ValMismatch": {
        "description": "ERROR:ValueMismatch (values do not match -- regardless of required or optional item)",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR_ValTypeMismatch": {
        "description": "ERROR:ValueTypeMismatch",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR_ValFormatMismatch": {
        "description": "ERROR:ValueFormatMismatch",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR": {
        "description": "ERROR:UnkownError",
        "icon": "remove-circle",
        "color": "#d50000"
    },
};

class Resolutions extends React.Component {
    constructor(props, context) {
        super(props, context);
    }
  
    render() {
        let icon = "ok-circle", color = "#f7f7f7", description, resolution;
        if(this.props.serverSideDiff) {
            resolution = this.props.serverSideDiff.resolution;
            icon = resolutionsIconMap[resolution].icon;
            color = resolutionsIconMap[resolution].color;
            description = resolutionsIconMap[resolution].description;
        }

        let tooltip = (
            <Tooltip id="tooltip">
              <p>{description}</p>
            </Tooltip>
        );
 
        return this.props.serverSideDiff && (
            <OverlayTrigger placement="top" overlay={tooltip}>
                <span>
                    <Glyphicon glyph="arrow-left" style={{color: this.props.removed && this.props.serverSideDiff ? "#ccc" : "#f7f7f7"}}/>
                    <Glyphicon glyph={icon} style={{color: color, "paddingRight": "9px", "paddingLeft": "9px"}}/>
                    <Glyphicon glyph="arrow-right" style={{color: this.props.added && this.props.serverSideDiff ? "#ccc" : "#f7f7f7"}}/>
                </span>
            </OverlayTrigger>
        );
    }
}

export default Resolutions;