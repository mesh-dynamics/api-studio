import React from 'react';
import { VIEW_TYPE } from "./GoldenVisibility";
import "./ViewType.css";

const ViewType = (props) => {

    const { viewType, setViewType } = props;

    return (
        <div className="gv-view-type-container">
            <span>View as : </span>
            <form className="gv-view-type-container-options">
                <div className="gv-view-type-option">
                    <input type="radio" name="gender" value="male" onChange={() => setViewType(VIEW_TYPE.TABLE)} checked={viewType === VIEW_TYPE.TABLE} /> 
                    <span className="gv-view-type-label">Table</span>
                </div>
                <div className="gv-view-type-option">
                    <input type="radio" name="gender" value="male"  onChange={() => setViewType(VIEW_TYPE.JSON)} checked={viewType === VIEW_TYPE.JSON} /> 
                    <span className="gv-view-type-label">JSON</span>
                </div>
            </form>
        </div>
    )
}

export default ViewType;
