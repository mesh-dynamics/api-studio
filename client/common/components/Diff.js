import React, { Component } from 'react';
import './Diff.css'

class Diff extends Component {
    constructor(props) {
        super(props);
    }

    getButton(resolution) {
        switch (resolution) {
            case 'OK_Optional':
                return (<span className="pull-right small orange-b">OPTIONAL</span>);
            case 'OK_OptionalMismatch':
                return (<span className="pull-right small orange-b">OPTIONAL MISMATCH</span>);
            case 'OK_OtherValInvalid':
                return (<span className="pull-right small orange-b">OPTIONAL VALUE INVALID</span>);
            case 'ERR_ValTypeMismatch':
                return (<span className="pull-right small red-b">ERROR VAL TYPE MISMATCH</span>);
            case 'OK':
                return (<span className="pull-right small green-b">OK</span>);
        }
    }

    formatDiff() {
        let {diff} = this.props;
        let formattedDiff = [];
        let pathList = [];
        for (const d of diff) {
            pathList.push(d.path);
        }

        for (let i =  0; i < diff.length; i++) {
            const d = diff[i];
            let pArr = pathList[i].split('/');
            pArr.shift();
            let li = pArr.pop();
            pArr =  pArr.join('--');
            let type = 'obj';
            if (parseInt(li) == li) {
                type = 'array'
            }

            if (d.type == 'obj') {
                formattedDiff.push({
                    pArr: pArr,
                    type: type,
                    children: [d]
                });
            } else {
                let result = formattedDiff.filter(obj => {
                    return obj.pArr === pArr
                });

                if (result && result[0] && result[0].children) {
                    result[0].children.push(d);
                } else {
                    formattedDiff.push({
                        pArr: pArr,
                        type: type,
                        children: [d]
                    });
                }
            }
        }

        return (formattedDiff);
    }

    render() {
        let {recorded, replayRes, diff} = this.props;
        const formattedDiff = this.formatDiff();

        let formattedDiffElements = formattedDiff.map((d, i, fde) => {
            const keys = d.pArr.indexOf('--') != -1 ? d.pArr.split('--') : [d.pArr];

            let childrenElem = d.children.map(c => {
                console.log(c.path);
                let ck = c.path.indexOf('/') != -1 ? c.path.split('/') : [c.path];
                let tempBef = JSON.parse(JSON.stringify(recorded));
                ck.shift();
                /*keys.shift();
                for (const key of ck) {
                    temp = temp[key];
                }*/

                for (let i = 0; i < ck.length; i++) {
                    tempBef = tempBef[ck[i]];
                }

                if (c.op == 'remove') {
                    return (
                        <div key={c.path} className="tabbed">
                            <span>{ck[ck.length-1] + ': '}</span>
                            {typeof c.value == 'string' ? (<span className="removed">{c.value}</span>) : (
                                <pre><div className="removed">{JSON.stringify(c.value, undefined, 4)}</div></pre>
                                )}
                                {this.getButton(c.resolution)}
                        </div>
                    )
                } else if (c.op == 'replace') {
                    return (
                        <div key={c.path} className="tabbed">
                            <span>{ck[ck.length-1] + ': '}</span>
                            <span className="removed">{c.fromValue}</span>&nbsp;
                            <span className="added">{c.value}</span>
                            {this.getButton(c.resolution)}
                        </div>
                    )
                } else if (c.op == 'add') {
                    return (
                        <div key={c.path} className="tabbed">
                            <span>{ck[ck.length-1] + ': '}</span>
                            <span className="removed">{c.fromValue ? c.fromValue : tempBef}</span>&nbsp;
                            <span className="added">{c.value}</span>
                            {this.getButton(c.resolution)}
                        </div>
                    )
                }
            });

            if (d.type == 'array') {
                return (
                    <div key={d.pArr}>
                        <div>{keys[keys.length-1] + ': ['}</div>
                        {childrenElem}
                        <div>]</div>
                    </div>
                )
            } else {
                return (
                    <div key={d.pArr}>
                        <div>{keys[keys.length-1] + ': {'}</div>
                        {childrenElem}
                        <div>}</div>
                    </div>
                )
            }
        });

        let diffElems = diff.map((d, i, darr) => {
            const keys = d.path.split('/');
            let temp = JSON.parse(JSON.stringify(recorded));
            let tempBef = JSON.parse(JSON.stringify(recorded));
            keys.shift();
            for (const key of keys) {
                temp = temp[key];
            }

            for (let i = 0; i < keys.length - 1; i++) {
                tempBef = tempBef[keys[i]];
            }

            if (d.op == 'remove') {
                return (
                    <pre>
                        {keys[keys.length-1] + ': '}
                        <span className="removed">
                            {JSON.stringify(temp, undefined, 4)}
                        </span>
                    </pre>
                )
            } else {
                return (
                    <pre>
                        {keys[keys.length-1] + ': '}
                        {JSON.stringify(tempBef, undefined, 4)}&nbsp;&nbsp;
                        {JSON.stringify(d.value, undefined, 4)}
                    </pre>
                )
            }

        });

        return (
            <div style={{marginTop: '20px'}}>
                {formattedDiffElements}
            </div>
        )
    }
}

export default Diff;
