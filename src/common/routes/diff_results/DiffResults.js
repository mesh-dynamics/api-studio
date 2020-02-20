import  React , { Component, Fragment } from "react";
import DiffResultsFilter from '../../components/DiffResultsFilter/DiffResultsFilter.js';
import DiffResultsList from '../../components/DiffResultsList/DiffResultsList.js';

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
            <div>
                <DiffResultsFilter filter={this.state.filter}></DiffResultsFilter>
                <DiffResultsList></DiffResultsList>
            </div>
        )
    } 
}
