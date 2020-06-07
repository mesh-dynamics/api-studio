import React, {Fragment, useEffect,useState } from 'react'
import { history } from '../../helpers';
import ReactTable from "react-table";  
import Modal from "react-bootstrap/lib/Modal";
import { cubeService } from "../../services";
import './APICatalog.css';
import _ from "lodash";

let tableData = {}

// TODO: refactor this into a class component which is compatible with react-table v6
const APIRequestsTable = (props) => {
    const {selectedService, selectedApiPath, apiTrace,app} = props;
    const [showModal, setShowModal] = useState(false);
    const [query,setQuery] = useState([]);
    const [form,setForm] = useState([]);
    const [details,setDetails] = useState([]); 

    const generateTableData = () => {
      tableData = {};
      apiTrace && !_.isEmpty(apiTrace.response) && apiTrace.response.forEach((trace) => {
        const requests = trace.res;
        const parentRequest = _.find(requests, {service: selectedService, apiPath: selectedApiPath});
        if (!parentRequest) {
          return
        }
        const outgoingRequests = requests.filter((req) => req.parentSpanId === parentRequest.spanId)
        const parentReqId = parentRequest.requestEventId;
        
        tableData[parentReqId] = {
            check: <input type="checkbox" className="requestBox" value={parentReqId}/>,  
            time: new Date(parentRequest.reqTimestamp * 1000).toLocaleString(),
            out: outgoingRequests.length ? outgoingRequests.map((outgoingRequest) => <div>{outgoingRequest.apiPath}</div>) : "NA",
            outgoingRequests: outgoingRequests,
        };
      });
    }

    const submitRequest = () => {
        const allCheckBox = Array.from(window.document.getElementsByClassName("requestBox"));
        const requestList = allCheckBox.filter((checkBox) => checkBox.checked)
                    .map((checkBox) => {
                      const reqId = checkBox.value;
                      const reqData = tableData[reqId];
                      const outgoingReqIds = reqData.outgoingRequests.map((req => req.requestEventId));
                      return `requestIds[${reqId}]=${outgoingReqIds.join(",")}`;
                    });
        history.push({
          pathname: "/http_client",
          search: `?app=${app}&${requestList.join("&")}`
        })
    }

    const selectAll=()=>{
      const checkBox = window.document.getElementById("selectAll");
      const allCheckBox = window.document.getElementsByClassName("requestBox");
      if(checkBox.checked){
        for(let i=0;i<allCheckBox.length;i++){
          allCheckBox[i].checked = true;
        }
      }
      else{
        for(let i=0;i<allCheckBox.length;i++){
          allCheckBox[i].checked = false;
        }
      }
  }

  const onCellClick=(rowInfo)=>{
    const requestId = rowInfo.original.check.props.value;

    cubeService.fetchAPIEventData(app, [requestId], ["HTTPRequest"])
    .then((result) => {
      setDetails(result.objects[0]);
      setQuery([result.objects[0].payload[1].queryParams]);
      setForm([result.objects[0].payload[1].formParams]);
      setShowModal(true);
    })
  }

  const handleClose = () => {
    setQuery([]);
    setShowModal(false);
  }

  const showDetails = ()=>{
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

  const makeTableQuery = ()=>{
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

  const makeTableForm = ()=>{
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

  const columns = 
    [
      {  
        Header: <input type="checkbox" id="selectAll" onChange={selectAll}></input>,
        columns:
        [
          {
            width:30,
            accessor: 'check',
            style:{
              textAlign:'center',
            }
          }
        ]
      },
      {  
        Header: <div style={{textAlign:"left",fontWeight:"bold"}}>TIME</div>,
        columns:
        [
          {
            accessor: 'time',
            getProps: (state, rowInfo) => ({
              onClick: () => onCellClick(rowInfo)
            }),
            style: {
              cursor: 'pointer',
            },
          }
        ]
      },
      {  
        Header: <div style={{textAlign:"left",fontWeight:"bold"}}>OUTGOING REQUESTS</div>,
        columns:
        [
          {
            accessor: 'out',

          }
        ]
      }
    ]
  

    return <div>
      <div>
      {generateTableData()}
        <ReactTable
            data={Object.values(tableData)}  
            columns={columns}  
            style={{height: "500px"}}
            defaultPageSize = {5}  
            pageSizeOptions = {[5, 10, 15, 20]}  
            className="-striped -highlight"
        />
      </div>
      <Modal show={showModal}>
        <Modal.Header>
          <Modal.Title>
            Request Details
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
        <div style={{overflowY:"auto", maxHeight:"250px"}}>
          {showDetails()}
          <label>Query Params:</label>
          <br/>
          {makeTableQuery()}
          <br/>
          <label>Form Params:</label>
          <br/>
          {makeTableForm()}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <div className="cube-btn text-center pull-right" style={{width:"100px"}} onClick={handleClose}>OK</div>
        </Modal.Footer>
      </Modal>
      <div>
          <div className="cube-btn text-center margin-top-10" style={{width:"100px"}} onClick={submitRequest}>VIEW</div>
      </div>
    </div>
}

export {APIRequestsTable};