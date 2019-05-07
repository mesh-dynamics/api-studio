import React, { Component } from 'react';
import {Clearfix, Table} from "react-bootstrap";
import DataTable from 'react-data-table-component';

class Analysis extends Component {
    constructor(props) {
        super(props);
    }

    formatData() {
        const {resByPath} =  this.props;
        let temp = [];
        let formattedList = [];
        for (const dp of resByPath) {
            if (dp.path && dp.service) {
                if (temp.indexOf(dp.service) == -1) {
                    temp.push(dp.service);
                }
            }
        }

        for (const key in temp) {

        }

        console.log(temp);
    }

    formatDataForTable(resByPath) {
        const formatted = [];
        for (const pathRes of resByPath) {
            if (pathRes.path) {
                formatted.push({
                    path: pathRes.path,
                    requests: pathRes.reqmatched + pathRes.reqpartiallymatched + pathRes.reqnotmatched,
                    respMatched: pathRes.respmatched + pathRes.resppartiallymatched,
                    respNotMatched: pathRes.respnotmatched,
                    incomplete: (pathRes.reqmatched + pathRes.reqpartiallymatched + pathRes.reqnotmatched) - (pathRes.respmatched + pathRes.resppartiallymatched + pathRes.respnotmatched),
                    reviewed: '-',
                    actions: '-'
                });
            }
        }

        return formatted;
    }

    render() {
        const {res, resByPath} =  this.props;
        this.formatData();
        const tableData = this.formatDataForTable(resByPath);
        const columns = [
            {
                name: 'Path',
                selector: 'path',
                sortable: true,
            },
            {
                name: 'Requests',
                selector: 'requests',
                sortable: true,
            },
            {
                name: 'Responses Matched',
                selector: 'respMatched',
                sortable: true,
            },
            {
                name: 'Responses Not Matched',
                selector: 'respNotMatched',
                sortable: true,
            },
            {
                name: 'Did Not Complete',
                selector: 'incomplete',
                sortable: true,
            },
            {
                name: 'Reviewed',
                selector: 'reviewed',
                sortable: false,
            },
            {
                name: 'Actions',
                selector: 'actions',
                sortable: false,
            }
        ];
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
            <DataTable
                columns={columns}
                data={tableData}
                pagination={true}
                striped={true}
                highlightOnHover={true}
            />
            {/*<Table striped bordered hover>
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
            </Table>*/}
        </div>)
    }
}

export default Analysis;
