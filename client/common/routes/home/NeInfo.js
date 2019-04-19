import React, { Component } from 'react'
import { Row, Col, Clearfix } from 'react-bootstrap'
import { XPanel, PageTitle } from '../../components'

class NeInfo extends Component {
    constructor(props) {
        super(props);
        this.donutChart = null;
        this.drawDonutChart = this.drawDonutChart.bind(this);
        this.updateDone = false;
    }
    render() {
        const { loadStatus } = this.props;
        return (
            <div>
                <Row>
                    <Col md={6} sm={6} xs={6}>
                        <XPanel>
                            <XPanel.Title title="NE Info" smallTitle="Device Type">
                            </XPanel.Title>
                            <XPanel.Content>
                                <div ref='donutChart'> </div>
                                {!(loadStatus == "SUCCESS") &&
                                    <img src="data:image/gif;base64,R0lGODlhEAAQAPIAAP///wAAAMLCwkJCQgAAAGJiYoKCgpKSkiH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGxvYWQuaW5mbwAh+QQJCgAAACwAAAAAEAAQAAADMwi63P4wyklrE2MIOggZnAdOmGYJRbExwroUmcG2LmDEwnHQLVsYOd2mBzkYDAdKa+dIAAAh+QQJCgAAACwAAAAAEAAQAAADNAi63P5OjCEgG4QMu7DmikRxQlFUYDEZIGBMRVsaqHwctXXf7WEYB4Ag1xjihkMZsiUkKhIAIfkECQoAAAAsAAAAABAAEAAAAzYIujIjK8pByJDMlFYvBoVjHA70GU7xSUJhmKtwHPAKzLO9HMaoKwJZ7Rf8AYPDDzKpZBqfvwQAIfkECQoAAAAsAAAAABAAEAAAAzMIumIlK8oyhpHsnFZfhYumCYUhDAQxRIdhHBGqRoKw0R8DYlJd8z0fMDgsGo/IpHI5TAAAIfkECQoAAAAsAAAAABAAEAAAAzIIunInK0rnZBTwGPNMgQwmdsNgXGJUlIWEuR5oWUIpz8pAEAMe6TwfwyYsGo/IpFKSAAAh+QQJCgAAACwAAAAAEAAQAAADMwi6IMKQORfjdOe82p4wGccc4CEuQradylesojEMBgsUc2G7sDX3lQGBMLAJibufbSlKAAAh+QQJCgAAACwAAAAAEAAQAAADMgi63P7wCRHZnFVdmgHu2nFwlWCI3WGc3TSWhUFGxTAUkGCbtgENBMJAEJsxgMLWzpEAACH5BAkKAAAALAAAAAAQABAAAAMyCLrc/jDKSatlQtScKdceCAjDII7HcQ4EMTCpyrCuUBjCYRgHVtqlAiB1YhiCnlsRkAAAOwAAAAAAAAAAAA==" />
                                }
                            </XPanel.Content>
                        </XPanel>
                    </Col>
                </Row>
            </div>
        )
    }


  
    componentWillUnmount() {
        const $ = window.$;
        const element = $(this.refs.donutChart);
        this.drawDonutChart(element, {});
    }
    
    componentDidUpdate() {
        const $ = window.$;
        const element = $(this.refs.donutChart);
        if(this.props.data && !this.updateDone) {
            this.updateDone = true
            this.drawDonutChart(element, this.props.data);
        }
    }
   

    
    drawDonutChart(divId, deviceInfo) {

        //if( typeof (Morris) === 'undefined'){ return; }
        //console.log('init_morris_charts');

        const $ = window.$;
        const Morris = window.Morris;
        function draw(deviceInfo) {
            var donut_data = [];
            //Get the list known to morris chart to get the donut chart
            for (var key in deviceInfo) {
                let deviceCount = deviceInfo[key];
                if (key == '-') {
                    key = 'Unknown';
                }
                donut_data.push({ label: key, value: deviceCount })
            }
            //console.log('dodata:'+ donut_data);

            if ($(divId).length) {
                Morris.Donut({
                    element: divId,
                    data: donut_data,
                    //This is the first 20 color list from Google Charts
                    colors: ['#3366CC', '#DC3912', '#FF9900', '#109618', '#990099',
                        '#3B3EAC', '#0099C6', '#DD4477', '#66AA00', '#B82E2E',
                        '#316395', '#994499', '#22AA99', '#AAAA11', '#6633CC',
                        '#E67300', '#8B0707', '#329262', '#5574A6', '#3B3EAC'],
                    formatter: function (y) {
                        return y;
                    },
                    resize: true
                });

            }

        };
        draw(deviceInfo);

    };
};

export default NeInfo;


