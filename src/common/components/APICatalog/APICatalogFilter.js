import React, { Component } from 'react'
import DatePicker from 'react-datepicker';

export default class APICatalogFilter extends Component {

    renderServiceDropdown = () => {
        const {handleFilterChange, services, selectedService} = this.props;
        const handleServiceDropDownChange = (event) => handleFilterChange("selectedService", event.target.value);

        return <select className="r-att" placeholder="Select Service" defaultValue={selectedService || "DEFAULT"} onChange={handleServiceDropDownChange}>   
            <option value="DEFAULT" disabled>Select Service</option>
            {services.map(service => 
                <option key={service.val} value={service.val}>
                    {service.val}
                </option>)
            }
        </select>;
    }

    renderAPIPathDropdown = () => {
        const {handleFilterChange, apiPaths, selectedApiPath} = this.props;
        const handleAPIDropDownChange = (event) => handleFilterChange("selectedApiPath", event.target.value);

        return <select className="r-att" placeholder="Select API" value={selectedApiPath} onChange={handleAPIDropDownChange}>
            {apiPaths.map(apiPath => <option key={apiPath.val} value={apiPath.val}>{apiPath.val}</option>)}
        </select>;
    }

    renderStartTime = () => {
        const {startTime, handleFilterChange} = this.props;
        return <DatePicker
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
            selected={new Date(endTime)}
            //todayButton="Today"
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

        return <select className="r-att" placeholder="Select Instance" value={selectedInstance} onChange={handleInstanceDropDownChange}>
            {instances.map(instance => <option key={instance.val} value={instance.val}>{instance.val}</option>)}
        </select>;
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
