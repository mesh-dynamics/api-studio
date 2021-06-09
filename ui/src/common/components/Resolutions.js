import React from 'react'
import { Glyphicon } from 'react-bootstrap'
import Tippy from '@tippy.js/react'
import 'tippy.js/themes/light-border.css'


const resolutionsIconMap = {
    "OK": {
        "description": "Value matched",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "OK_DefaultCT": {
        "description": "Value mismatch: Ignored due to default",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "ERR_ValTypeMismatch": {
        "description": "Error: Data type mismatch",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "OK_DefaultPT": {
        "description": "Missing item in test: Ignored due to default",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "ERR_NewField": {
        "description": "Error: New item in test",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "OK_CustomMatch": {
        "description": "Value matched",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "ERR_ValFormatMismatch": {
        "description": "Error: Value format mismatch",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR_ValMismatch": {
        "description": "Error: Value mismatch",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "OK_Ignore": {
        "description": "Value ignored",
        "icon": "ok-circle",
        "color": "#5efc82"
    },
    "ERR_Required": {
        "description": "Error: Missing required item in test",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "OK_Optional": {
        "description": "Missing optional item in test",
        "icon": "warning-sign",
        "color": "#ffdd4b"
    },
    "OK_OptionalMismatch": {
        "description": "Missing optional item in test",
        "icon": "warning-sign",
        "color": "#ffdd4b"
    },
    "ERR_RequiredGolden": {
        "description": "Error: Missing required item in golden",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "OK_OtherValInvalid": {
        "description": "Missing optional item in golden",
        "icon": "warning-sign",
        "color": "#ffdd4b"
    },
    "ERR_InvalidExtractionMethod": {
        "description": "Error: Data does not match rule",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR_RequiredBoth": {
        "description": "Error: Missing required item in golden and test",
        "icon": "remove-circle",
        "color": "#d50000"
    },
    "ERR": {
        "description": "Error",
        "icon": "remove-circle",
        "color": "#d50000"
    }
};

class Resolutions extends React.Component {
    constructor(props, context) {
        super(props, context);
    }

    render() {
        let icon = "ok-circle", color = "#f7f7f7", description, resolution;
        if (this.props.serverSideDiff) {
            resolution = this.props.serverSideDiff.resolution;
            icon = resolutionsIconMap[resolution] ? resolutionsIconMap[resolution].icon : "warning-sign";
            color = resolutionsIconMap[resolution] ? resolutionsIconMap[resolution].color : "#ffdd4b";
            description = resolutionsIconMap[resolution] ? resolutionsIconMap[resolution].description : resolution;
        }

        let tippyContent = (
            <div style={{fontSize: "14px"}}>
                <p>{description}</p>
            </div>
        );

        return this.props.serverSideDiff ? (
            <Tippy content={tippyContent} arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light-border"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true}>
                <span><Glyphicon glyph={icon} style={{ color: color, "paddingRight": "9px", "paddingLeft": "9px" }} /></span>
            </Tippy>
        ) :
            (<span></span>
        );
    }
}

export {resolutionsIconMap};
export default Resolutions;