import React, { Component } from 'react'
import { Link } from 'react-router-dom'

// incoming/outgoing requests table
const APIListTable = (props) => {
    const {app, apiPaths, selectedService, startTime, endTime} = props;

    const startTimeISO = new Date(startTime).toISOString();
    const endTimeISO = new Date(endTime).toISOString();

    return(
        <div style={{margin: "10px", width:"100%"}}>
        <h5>INCOMING</h5>
            <table className="table table-bordered table-striped">
                <thead>
                    <tr>
                        <th style={{width:"80%"}}>
                            API
                        </th>
                        <th style={{width:"20%"}}>
                            Count
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