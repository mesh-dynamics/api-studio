import React, { Component } from 'react'
import { history } from '../../helpers';
import ReactTable from "react-table";  
import Modal from "react-bootstrap/lib/Modal";
import { cubeService } from "../../services";
import './APICatalog.css';
import _ from "lodash";
import { apiCatalogActions } from '../../actions/api-catalog.actions';
import { connect } from "react-redux";

class APIRequestsTable extends Component {

  constructor (props) {
    super(props);

    this.state = {
      showModal : false,
      query : [],
      form : [],
      details : [],
      tableData: [],
      selectAllChecked: false,
    }
  }



  componentWillReceiveProps (nextProps) {
    const tableData = this.generateTableData(nextProps);
    this.setState({tableData})
  }

  generateTableData = (props) => {
    const {selectedInstance, app, selectedApiPath, selectedService, apiTrace} = props;
    if (apiTrace && !_.isEmpty(apiTrace.response)) {
      return apiTrace.response.map((trace) => {
          const requests = trace.res;
          const parentRequest = _.find(requests, {service: selectedService, apiPath: selectedApiPath});
          if (!parentRequest) {
            return
          }
          const parentReqId = parentRequest.requestEventId;
          const outgoingRequests = requests.filter((req) => req.parentSpanId === parentRequest.spanId)

          return {
              parentRequest: parentRequest,
              outgoingRequests: outgoingRequests,
              parentReqId: parentReqId,
              instance: selectedInstance,
              checked: false,
          };
      }).filter(r => r); // filter out undefined objects
    } else {
      return [];
    }
  }

  generateTableRows = () => {
    const {tableData} = this.state;
    const {apiCatalog} = this.props;

    if (_.isEmpty(tableData)) {
      return [];
    }
    
    return tableData
            .map((traceData) => ({
              check: <input type="checkbox" value={traceData.parentReqId} checked={traceData.checked} onChange={this.handleRowCheckChanged}/>,
              time: traceData.parentRequest.reqTimestamp,
              out: traceData.outgoingRequests.length ? traceData.outgoingRequests.map((outgoingRequest) => <div>{outgoingRequest.apiPath}</div>) : "NA", // todo stylize
              compare: <label onClick={() => this.handleCompareSelect(traceData.parentReqId)}><i className="fas fa-1x fa-thumb-tack" style={{cursor: "pointer", color: _.find(apiCatalog.compareRequests, {parentReqId: traceData.parentReqId}) ? "black": "grey", fontSize: "large",}}></i></label>,
            }));
  }

  handleRowCheckChanged = (e) => {
    const {tableData} = this.state;
    const traceData = _.find(tableData,  {parentReqId: e.target.value})
    traceData.checked = !traceData.checked;
    this.setState({tableData})
  }

  handleCompareSelect = (parentReqId) => {
    const {tableData} = this.state;
    const {dispatch, apiCatalog} = this.props;
    
    const reqData = _.find(tableData, {parentReqId: parentReqId});
    
    // if already present in compare data array, unpin it, else pin it
    if (_.find(apiCatalog.compareRequests, {parentReqId: parentReqId})) {
      dispatch(apiCatalogActions.unpinCompareRequest(reqData));
    } else {
      dispatch(apiCatalogActions.pinCompareRequest(reqData));
    }
  }

  handleViewRequests = () => {
    const {tableData} = this.state;
    const {app} = this.props;
      
    const requestList = tableData
                .filter(r => r.checked)
                .map((traceData) => {
                  const reqId = traceData.parentReqId;
                  const outgoingReqIds = traceData.outgoingRequests.map((req => req.requestEventId));
                  return `requestIds[${reqId}]=${outgoingReqIds.join(",")}`;
                });
    
    history.push({
      pathname: "/http_client",
      search: `?app=${app}&${requestList.join("&")}`
    })
  }

  selectAllCheckChanged = (e) => {
    let {tableData, selectAllChecked} = this.state;
    selectAllChecked = !selectAllChecked;
    tableData.forEach(v => {v.checked = selectAllChecked}); // change checked state for all
    this.setState({tableData, selectAllChecked});
  }

  onCellClick=(rowInfo)=>{
    const {app} = this.props;
    const requestId = rowInfo.original.check.props.value;

    cubeService.fetchAPIEventData(app, [requestId], ["HTTPRequest"])
    .then((result) => {
      this.setState({
        details:result.objects[0],
        query:[result.objects[0].payload[1].queryParams],
        form:[result.objects[0].payload[1].formParams],
        showModal:true
      })
    })
  }

  handleClose = () => {
    this.setState({
      query:[],
      showModal:false
    })
  }

  showDetails = ()=>{
    const {details}=this.state;
    const date = new Date(details.timestamp*1000);
    const dateString = date.toLocaleString();
    return (
      <div>
        <label>API Path: </label> {details.apiPath}
        <br/>
        <label>Timestamp: </label> {dateString}
      </div>
    )
  }

  makeTableQuery = ()=>{
    const {query}=this.state;
    try{
      const keys = Object.keys(query[0]);
      const values = Object.values(query[0]);
      if(keys.length>0){
        return (
          <table className="Rtable">
          <tr>
              <th style={{width: "20%"}}>KEY</th>
              <th style={{width: "80%"}}>VALUE</th>
          </tr>
          {keys.map((result,index)=>{
            return(
            <tr>
              <td>{result}</td>
              <td>{values[index]}</td>
            </tr>
            ) 
          })}
          </table>
      )
      }
      else{
        return "No Data"
      }
    }
    catch(e){
      console.log(e);
    }
    
  }

  makeTableForm = ()=>{
    const {form} = this.state;
    try{
      const keys = Object.keys(form[0]);
      const values = Object.values(form[0]);
      if(keys.length>0){
        return (
            <table className="Rtable">
            <tr>
              <th style={{width: "20%"}}>KEY</th>
              <th style={{width: "80%"}}>VALUE</th>
            </tr>
            {keys.map((result,index)=>{
              return(
              <tr>
                <td>{result}</td>
                <td>{values[index]}</td>
              </tr>
              ) 
            })}
            </table>
        )
      }
      else{
        return "No Data"
      }
    }
    catch(e){
      console.log(e);
    }
    
  }


  generateTableColumns = () => {
    const {selectAllChecked} = this.state;
    
    return [
      {  
        Header: <input type="checkbox" onChange={this.selectAllCheckChanged} value={selectAllChecked}></input>,
        columns: [
          {
            width:30,
            accessor: 'check',
            style: {
              textAlign: 'center',
            }
          }
        ]
      },
      {  
        Header: <div style={{textAlign:"left",fontWeight:"bold"}}>TIME</div>,
        columns: [
          {
            id: r => r.time ,
            accessor: r => new Date(r.time * 1000).toLocaleString(), // todo
            getProps: (state, rowInfo) => ({
              onClick: () => this.onCellClick(rowInfo)
            }),
            style: {
              cursor: 'pointer',
            },
          }
        ]
      },
      {  
        Header: <div style={{textAlign:"left",fontWeight:"bold"}}>OUTGOING REQUESTS</div>,
        columns: [
          {
            accessor: 'out',

          }
        ]
      },
      {  
        Header: <div style={{textAlign:"left",fontWeight:"bold"}}>COMPARE</div>,
        columns: [
          {
            accessor: 'compare',
            width: 70,
            style: {
              textAlign: 'center',
            },
          }
        ]
      }
    ]
  }
  
  generateModals = () => {
    const {showModal} = this.state;
    
    return (
      <Modal show={showModal}>
        <Modal.Header>
          <Modal.Title>
            Request Details
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div style={{overflowY:"auto", maxHeight:"250px"}}>
            {this.showDetails()}
            <label>Query Params:</label>
            <br/>
            {this.makeTableQuery()}
            <br/>
            <label>Form Params:</label>
            <br/>
            {this.makeTableForm()}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <div className="cube-btn text-center pull-right" style={{width:"100px"}} onClick={this.handleClose}>OK</div>
        </Modal.Footer>
      </Modal>
    );
  }

  render() {

    return (
      <div>
        <div>
          <ReactTable
              data={this.generateTableRows()}  
              columns={this.generateTableColumns()}  
              style={{height: "500px"}}
              defaultPageSize = {5}  
              pageSizeOptions = {[5, 10, 15, 20]}  
              className="-striped -highlight"
          />
        </div>
        
        <div>
            <div className="cube-btn text-center margin-top-10" style={{width:"100px"}} onClick={this.handleViewRequests}>VIEW</div>
        </div>

        {this.generateModals()}
      </div>
    )
  }

}

const mapStateToProps = (state) => ({
  cube: state.cube,
  apiCatalog: state.apiCatalog,
});

const connectedAPIRequestsTable = connect(mapStateToProps)(APIRequestsTable);

export default connectedAPIRequestsTable;
export {connectedAPIRequestsTable as APIRequestsTable}