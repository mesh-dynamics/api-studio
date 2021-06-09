import React, {useState} from 'react';
import{ APICatalogTopFilters} from './APICatalogTopFilters'
import { APICatalogServiceGraph } from './APICatalogServiceGraph';

/* 
export interface ICollapsibleTabs{ 
    currentPage
}
*/

export default function CollapsibleTabs(props){
    const [showGraph, setShowGraph] = useState(false);
    const [showFilters, setShowFilters] = useState(true);
    
    const showHideGraph =() => {
        setShowFilters(false);
        setShowGraph(u=> !u);
    }
    const showHideFilters = () => {        
        setShowFilters(u=> !u);
        setShowGraph(false);
    }


    return(
        <div className="api-catalog-bordered-bottom top-api-nav" >
            <div>
                <div className={"nav-item " + (showGraph ? " selected":"")}  onClick={showHideGraph}>SERVICE GRAPH&nbsp;
                <span>
                    {showGraph ? 
                    (<i className="fa fa-chevron-circle-up"/> )
                    :
                    (<i className="fa fa-chevron-circle-down"/>)
                    }
                    
                </span>
                </div>
                {props.currentPage == "api" && <div  className={"nav-item " + (showFilters ? " selected":"")}  onClick={showHideFilters} >FILTERS&nbsp;
                    <span>
                        {showFilters ? 
                        (<i className="fa fa-chevron-circle-up"/> )
                        :
                        (<i className="fa fa-chevron-circle-down"/>)
                        }
                        
                    </span>
                </div>}
                </div>
                {showGraph && <APICatalogServiceGraph />}
                {props.currentPage == "api" && showFilters && <APICatalogTopFilters /> }
            </div>
    )
}