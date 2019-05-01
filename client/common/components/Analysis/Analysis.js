import React, { Component } from 'react';
import {Clearfix, Table} from "react-bootstrap";

class Analysis extends Component {
    constructor(props) {
        super(props);
    }

    formatData() {
        const {resByPath} =  this.props;
        let temp = [];
        for (const dp of resByPath) {
            if (dp.path && dp.service) {
                if (temp.indexOf(dp.service) == -1) {
                    temp.push(dp.service);
                }
            }
        }

        console.log(temp);
    }

    render() {
        const {res, resByPath} =  this.props;
        this.formatData();
        let errorByPath = resByPath.map(pathRes => {
            if (pathRes.path) {
                return (
                    <tr>
                        <td>{pathRes.path ? pathRes.path : '-'}</td>
                        <td>{pathRes.reqmatched + pathRes.reqpartiallymatched + pathRes.reqnotmatched}</td>
                        <td>{pathRes.respmatched + pathRes.resppartiallymatched}</td>
                        <td>{pathRes.respnotmatched}</td>
                        <td>{(pathRes.reqmatched + pathRes.reqpartiallymatched + pathRes.reqnotmatched) - (pathRes.respmatched + pathRes.resppartiallymatched + pathRes.respnotmatched)}</td>
                        <td>-</td>
                        <td>-</td>
                    </tr>
                );
            } else {
                return null;
            }
        });

        return (<div style={{marginTop: '20px'}}>
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
