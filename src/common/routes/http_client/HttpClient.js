import  React , { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Glyphicon } from 'react-bootstrap';
import _ from 'lodash';
import axios from "axios";
import "./HttpClient.css"
import {cubeActions} from "../../actions";
import {cubeConstants} from "../../constants";

class HttpClient extends Component {

    

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(false));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(true));
    }

    

    render() {
        const { cube } = this.props;
        return (
            <div className="content-wrapper">
                <div>
                    <div className="vertical-middle inline-block">
                        <svg height="21"  viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0"/>
                        </svg>
                    </div>
                    <div className="inline-block vertical-middle" style={{fontWeight: "bold", position: "relative", bottom: "3px", opacity: "0.5", paddingLeft: "10px"}}>API CATALOG - VIEW REQUEST DETAILS</div>
                </div>

                <div>
                    <FormGroup>
                        <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                            type="text"
                            value=""
                            placeholder="Search"
                        />
                    </FormGroup>
                </div>

                <div className="tab-wrapper" style={{width: "100%"}}>
                    <div className="tabs">
                        <div className="tab active">
                            <div>
                                <div className="tab-head">minfo/ListMoviews?film...</div>
                            </div>
                        </div>
                        <div className="tab inactive">
                            <div>
                                <div className="tab-head">minfo/ListMoviews?film...</div>
                            </div>
                        </div>
                    </div>
                    <div className="tab-last">
                        <div style={{width: "16px"}}>
                            <div className="tab-head">
                                <Glyphicon glyph="plus" />
                            </div>
                        </div>
                    </div>
                </div>
                <div class="tab-body-wrapper">

                </div>
                
            </div>
        );
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedHttpClient = connect(mapStateToProps)(HttpClient);

export default connectedHttpClient
