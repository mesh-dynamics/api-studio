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
