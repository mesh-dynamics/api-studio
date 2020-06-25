import React, { Component } from 'react'
import './APICatalog.css';

// incoming/outgoing requests table
const APICountTable = (props) => {
    const {apiCount} = props;

    return(
        <div style={{width:"60%"}}>
            <table className="table">
                <thead>
                    <tr>
                        <th style={{width:"80%", fontWeight: "normal"}}>
                            SERVICE
                        </th>
                        <th style={{width:"20%", fontWeight: "normal"}}>
                            COUNT
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>
                            All
                        </td>
                        <td>
                            {apiCount}
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    )
}

export {APICountTable};