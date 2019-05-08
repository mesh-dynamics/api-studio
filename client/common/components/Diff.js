import React, { Component } from 'react';

class Diff extends Component {
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

    render() {
        let {recorded, replayRes, diff} = this.props;

        return (
            <div style={{marginTop: '20px'}}>
                This is diff
            </div>
        )
    }
}

export default Diff;
