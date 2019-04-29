import React, { Component } from 'react';
import {Clearfix, Table} from "react-bootstrap";

class Analysis extends Component {
    constructor(props) {
        super(props);
    }

    render() {
        const {res, resByPath} =  this.props;

        let errorByPath = resByPath.map(pathRes => {
            if (pathRes.path) {
                return (
                    <tr>
                        <td>{pathRes.path ? pathRes.path : '-'}</td>
                        <td>{res.reqcnt}</td>
                        <td>{pathRes.respmatched + pathRes.resppartiallymatched}</td>
                        <td>{pathRes.respnotmatched}</td>
                        <td>{res.reqcnt - (pathRes.respmatched + pathRes.resppartiallymatched + pathRes.respnotmatched)}</td>
                        <td>-</td>
                        <td>-</td>
                    </tr>
                );
            } else {
                return null;
            }
        });

        return (<div>
            <Table striped bordered hover>
                <thead>
                <tr>
                    <th className="default">Path</th>
                    <th className="default">Requests</th>
                    <th className="default">Responses Matched</th>
                    <th className="default">Responses Not Matched</th>
                    <th className="default">Did Not Complete</th>
                    <th className="default">Reviewed</th>
                    <th className="default">Actions</th>
                </tr>
                </thead>
                <tbody>
                {errorByPath}
                </tbody>
            </Table>
        </div>)
    }
}

export default Analysis;
