import React, { Component } from 'react'
import DatePicker from 'react-datepicker';
import './APICatalog.css';


export default class APICatalogFilter extends Component {

    renderServiceDropdown = () => {
        const {handleFilterChange, services, selectedService} = this.props;
        const handleServiceDropDownChange = (event) => handleFilterChange("selectedService", event.target.value);

        return (
        <div>
            <select className="r-att form-control" placeholder="Select Service" value={selectedService || "DEFAULT"} onChange={handleServiceDropDownChange}>   
                <option value="DEFAULT" disabled>Select Service</option>
                {services.map(service => 
                    <option key={service.val} value={service.val}>
                        {service.val}
                    </option>)
                }
            </select>
        </div>);
    }

    renderDayPicker = () => {
        const {prevDays} = this.props;
        return (
            <div>
                <input type="number" defaultValue={prevDays} onKeyPressCapture={this.handlePrevDaysChange} className="text-center form-control" min="1"/>
            </div>
        )
    }

    renderAPIPathDropdown = () => {
        const {handleFilterChange, apiPaths, selectedApiPath} = this.props;
        const handleAPIDropDownChange = (event) => handleFilterChange("selectedApiPath", event.target.value);

        return <select className="r-att form-control" placeholder="Select API" value={selectedApiPath} onChange={handleAPIDropDownChange}>
            {apiPaths.map(apiPath => <option key={apiPath.val} value={apiPath.val}>{apiPath.val}</option>)}
        </select>;
    }

    renderStartTime = () => {
        const {startTime, handleFilterChange} = this.props;
        return <DatePicker
            className="form-control"
            selected={new Date(startTime)}
            //todayButton="Today"
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={15}
            timeCaption="time"
            dateFormat="yyyy/MM/dd HH:mm"
            onChange={dateTime => handleFilterChange("startTime", dateTime)}
        />;
    }

    renderEndTime = () => {
        const {endTime, handleFilterChange} = this.props;
        return <DatePicker
        className="form-control"
        selected={new Date(endTime)}
            //todayButton="Today"
            className="form-control"
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={15}
            timeCaption="time"
            dateFormat="yyyy/MM/dd HH:mm"
            onChange={dateTime => handleFilterChange("endTime", dateTime)}
        />;
    }

    renderInstanceDropdown = () => {
        const {handleFilterChange, instances, selectedInstance} = this.props;
        const handleInstanceDropDownChange = (event) => handleFilterChange("selectedInstance", event.target.value);

        return <select className="r-att form-control" placeholder="Select Instance" value={selectedInstance} onChange={handleInstanceDropDownChange}>
            {instances.map(instance => <option key={instance.val} value={instance.val}>{instance.val}</option>)}
        </select>;
    }

    handlePrevDaysChange = (e) => {
        let value = parseInt(e.target.value)
          if (e.key !== "Enter")
              return;
  
          if (value <= 0) {
              alert("Invalid prevDays value")
              console.error("Invalid prevDays value")
              return;
          }
          this.props.handlePrevDaysChange(value);
      }
  

    render() {
        const {currentPage, app} = this.props;
        return (
            <div>
                <div>
                    <div className="label-n">APPLICATION</div>
                    <div className="application-name">{app}</div>
                </div>

                <div className="margin-top-10">
                    <div className="label-n">SELECT SERVICE</div>
                    {this.renderServiceDropdown()}
                </div>

                {
                    (currentPage==="landing" || currentPage==="service") && 
                    <div className="margin-top-10">
                        <div className="label-n">START FROM (DAYS)</div>
                        {this.renderDayPicker()}
                    </div>
                }
                {
                    currentPage==="api" && 
                    <div>
                        <div className="margin-top-10">
                            <div className="label-n">API</div>
                            {this.renderAPIPathDropdown()}
                        </div>

                        <div className="margin-top-10">
                            <div className="label-n">START TIME</div>
                            {this.renderStartTime()}
                        </div>

                        <div className="margin-top-10">
                            <div className="label-n">END TIME</div>
                            {this.renderEndTime()}
                        </div>

                        <div className="margin-top-10">
                            <div className="label-n">SOURCE INSTANCE</div>
                            {this.renderInstanceDropdown()}
                        </div>
                    </div>
                }

            </div>
        )
    }
}
