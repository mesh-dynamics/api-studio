import React, { Component } from 'react'
import { Link } from 'react-router-dom'
import './APICatalog.css';

// incoming/outgoing requests table
const APIListTable = (props) => {
    const {app, apiPaths, selectedService, startTime, endTime} = props;

    const startTimeISO = new Date(startTime).toISOString();
    const endTimeISO = new Date(endTime).toISOString();

    return(
        <div style={{width:"100%"}}>
            <table className="table">
                <thead>
                    <tr>
                        <th style={{width:"80%", fontWeight:"normal"}}>
                            API
                        </th>
                        <th style={{width:"20%", fontWeight:"normal"}}>
                            COUNT
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {apiPaths.map(api => {
                        return (
                                <tr>
                                    <td>
                                        <Link to={{
                                            pathname: "api",
                                            search: `?app=${app}&selectedService=${selectedService}&selectedAPI=${api.val}&startTime=${startTimeISO}&endTime=${endTimeISO}`
                                        }}>
                                            {api.val}
                                        </Link>
                                    </td>
                                    <td>
                                        {api.count}
                                    </td>
                                </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    )
}

export {APIListTable};