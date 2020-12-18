import React, { Component } from 'react'
import './APICatalog.scss';
export interface IAPICatalogLandingProps{
    setCurrentPage: (page: string)=>void;
}
export default class APICatalogLanding extends Component<IAPICatalogLandingProps> {

    componentDidMount() {
        this.props.setCurrentPage("landing")
    }

    render() {
        return (
            <div></div>
        )
    }
}
