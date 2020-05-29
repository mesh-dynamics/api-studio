import React, { useEffect } from 'react'
import { history } from '../../helpers';
import ReactTable from "react-table";  
import "react-table/react-table.css";  


let tableData = []

const APIRequestsTable = (props) => {
    const {selectedService, selectedApiPath, apiTrace} = props;

    const generateTableData = ()=>{
      tableData = [];
      // console.log(apiTrace);
        try{
          {apiTrace.response.map((req,index) => {
            if(req.res.length>1){
              if(req.res[0].service === selectedService && req.res[0].apiPath === selectedApiPath){
                if(req.res[1].parentSpanId === req.res[0].spanId){
                  const date = new Date(req.res[1].reqTimestamp*1000);
                  const dateString = date.toLocaleString();
                  tableData.push(
                    {
                      check: <input type="checkbox" className="requestBox" value={req.res[1].requestEventId}></input>,  
                      time: dateString,
                      out:req.res.map((result, index) => {
                        if(req.res[0].spanId === req.res[index].parentSpanId){
                            return  <div>{req.res[index].apiPath}</div>
                          }
                      })
                    }
                  )
                }
              }
            }
            else if(req.res.length===1){
              if(req.res[0].service === selectedService && req.res[0].apiPath === selectedApiPath){
                  const date = new Date(req.res[0].reqTimestamp*1000);
                  const dateString = date.toLocaleString();
                  tableData.push(
                    {
                      check: <input type="checkbox" className="requestBox" value={req.res[0].requestEventId}></input>,  
                      time: dateString,
                      out:"-"
                    }
                  )
              }
            }
        })
      }
      }
      catch(e){
        console.log(e);
      }
    }

    const submitRequest=()=>{
        const allCheckBox = window.document.getElementsByClassName("requestBox");
        let requestList = "";
        for(let i=0;i<allCheckBox.length;i++){
            if(allCheckBox[i].checked){
                requestList += allCheckBox[i].value +",";
            }
        }
        requestList = requestList.substring(0,requestList.length-1);
        // /httpclient?requestIds=id1,id2
        history.push({
          pathname: "/http_client",
          search: `?requestIds=${requestList}`
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

 const columns = 
 [
   {  
      Header: <input type="checkbox" id="selectAll" onChange={selectAll}></input>,
      columns:
      [
        {
          width:30,
          accessor: 'check',
        }
      ]
   },
   {  
      Header: "Time",
      columns:
      [
        {
          accessor: 'time',
        }
      ]
    },
    {  
      Header: "Outgoing Requests",
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
      {console.log(tableData)}
        <ReactTable
            data={tableData}  
            columns={columns}  
            style={{height: "500px"}}
            defaultPageSize = {5}  
            pageSizeOptions = {[5, 10, 15, 20]}  
            className="-striped -highlight"
        />
      </div>
      <div>
          <button style={{marginLeft:"10px"}} type="button" onClick={submitRequest}>View</button>
      </div>
    </div>
}

export {APIRequestsTable};