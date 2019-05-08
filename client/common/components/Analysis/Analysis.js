import React, { Component } from 'react';
import {Clearfix, Table} from "react-bootstrap";
import DataTable from 'react-data-table-component';
import Diff from "../Diff";

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
        const recordedResponse = [{"actors_lastnames":["HARRIS","WILLIS","TEMPLE"],"display_actors":["DAN HARRIS","HUMPHREY WILLIS","BURT TEMPLE"],"film_id":851,"title":"STRAIGHT HOURS","actors_firstnames":["DAN","HUMPHREY","BURT"],"film_counts":[28,26,23],"timestamp":1641491700530174,"book_info":{"reviews":[{"reviewer":"Reviewer1","text":"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!"},{"reviewer":"Reviewer2","text":"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare."}],"id":"851"}}];
        const replayRes = [{"display_actors":["HARRIS,DAN","WILLIS,HUMPHREY","TEMPLE,BURT"],"film_id":851,"title":"STRAIGHT HOURS","actors_firstnames":["DAN","BURT","HUMPHREY"],"film_counts":["28","23","26"],"timestamp":27523407007561}];
        const diff = [
            {"op":"remove","path":"/0/actors_lastnames","value":["HARRIS","WILLIS","TEMPLE"],"resolution":"OK_Optional"},
            {"op":"replace","path":"/0/display_actors/0","value":"HARRIS,DAN","fromValue":"DAN HARRIS","resolution":"OK_OptionalMismatch"},
            {"op":"replace","path":"/0/display_actors/1","value":"WILLIS,HUMPHREY","fromValue":"HUMPHREY WILLIS","resolution":"OK_OptionalMismatch"},
            {"op":"replace","path":"/0/display_actors/2","value":"TEMPLE,BURT","fromValue":"BURT TEMPLE","resolution":"OK_OptionalMismatch"},
            {"op":"add","path":"/0/actors_firstnames/1","value":"BURT","resolution":"OK_OtherValInvalid"},
            {"op":"remove","path":"/0/actors_firstnames/3","value":"BURT","resolution":"OK"},
            {"op":"replace","path":"/0/film_counts/0","value":"28","fromValue":28,"resolution":"ERR_ValTypeMismatch"},
            {"op":"replace","path":"/0/film_counts/1","value":"23","fromValue":26,"resolution":"ERR_ValTypeMismatch"},
            {"op":"replace","path":"/0/film_counts/2","value":"26","fromValue":23,"resolution":"ERR_ValTypeMismatch"},
            {"op":"replace","path":"/0/timestamp","value":27523407007561,"fromValue":1641491700530174,"resolution":"OK_OptionalMismatch"},
            {"op":"remove","path":"/0/book_info","value":{"reviews":[{"reviewer":"Reviewer1","text":"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!"},{"reviewer":"Reviewer2","text":"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare."}],"id":"851"},"resolution":"OK"}
        ];
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

        return (<div>
            <DataTable
                columns={columns}
                data={tableData}
                pagination={true}
                striped={true}
                highlightOnHover={true}
            />

            <Diff recorded={recordedResponse} replayRes={replayRes} diff={diff}/>
        </div>)
    }
}

export default Analysis;
