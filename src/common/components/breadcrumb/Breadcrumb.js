import React, { Component } from 'react';

class Breadcrumb extends Component {
    render() {
        const { crumbs } = this.props;
        let jsxContent = crumbs.map(item => {
            return (
                <div className="crumb-unit">
                    <div className="label-n">{item.label}</div>
                    <div className="value-n">{item.value}</div>
                </div>
            );
        });

        return <div className="margin-bottom-10">{jsxContent}</div>;
    }
}

export default Breadcrumb;
