import React, { Component } from 'react'
import './APICatalog.scss';

export default class APICatalogLanding extends Component {

    constructor(props) {
        super(props);
    }

    componentDidMount() {
        this.props.setCurrentPage("landing")
    }

    render() {
        return (
            <div></div>
        )
    }
}
