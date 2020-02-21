import  React , { Component, Fragment } from "react";
import DiffResultsFilter from '../../components/DiffResultsFilter/DiffResultsFilter.js';
import DiffResultsList from '../../components/DiffResultsList/DiffResultsList.js';
import { Glyphicon} from 'react-bootstrap';
import {Link} from "react-router-dom";

export default class DiffResults extends Component {
    constructor(props) {
        super(props);
        this.state = {
            filter : {
                service: "",
                apiPath: "",
                reqRespMatchType: "",
                resolutionType: "",
                pageNumber: 1,
            }
        }
    }

    handleFilterChange = () => {
        
    }

    render() {
        return (
            <div className="content-wrapper">
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <Link to={"/"} onClick={this.handleBackToDashboardClick}><span className="link-alt"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
                    <span className="link-alt pull-right" onClick={this.showSaveGoldenModal}>&nbsp;&nbsp;&nbsp;&nbsp;<i className="fas fa-save font-15"></i>&nbsp;Save Golden</span>
                    <Link to="/review_golden_updates" className="hidden">
                        <span className="link pull-right"><i className="fas fa-pen-square font-15"></i>&nbsp;REVIEW GOLDEN UPDATES</span>
                    </Link>
                </div>
                <div>
                    <DiffResultsFilter filter={this.state.filter}></DiffResultsFilter>
                    <DiffResultsList></DiffResultsList>
                </div>
            </div>
        )
    } 
}
