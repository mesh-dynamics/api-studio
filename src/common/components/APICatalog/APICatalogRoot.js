import React, { Component } from 'react'
import {FormControl}  from "react-bootstrap";

export default class APICatalogRoot extends Component {
    render() {
        return (
            <div style={{display:"flex", flexDirection: "column", margin: "10px"}}>
                <FormControl
                    type="input"
                    // onChange={}
                    placeholder="Search API Catalog"
                />
            </div>
        )
    }
}
