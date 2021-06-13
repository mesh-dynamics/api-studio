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

const REQUEST_RULE = {
    MATCH: "request::match::rules",
    COMPARE: "request::compare:rules"
}

const REQUEST_TABS = {
    HEADERS: "request::headers",
    FORM_PARAMS: "request::form::params",
    QUERY_PARAMS: "request::query::params",
    BODY: "request::body",
    EXAMPLES: "request::examples",
};

const RESPONSE_TABS = {
    BODY: "response::body",
    EXAMPLES: "response::examples",
};

const VIEW = {
    GOLDEN_SUMMARRY: "view::golden::summary"
};

const VIEW_TYPE = {
    TABLE: "viewtype::table",
    JSON: "viewtype::json"
};

const CONTRACT_TYPE = {
    REQUEST: "contract::request",
    RESPONSE: "contract::response"
}

export { 
    VIEW, 
    VIEW_TYPE,
    REQUEST_RULE, 
    REQUEST_TABS, 
    RESPONSE_TABS,
    CONTRACT_TYPE
};
