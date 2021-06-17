/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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