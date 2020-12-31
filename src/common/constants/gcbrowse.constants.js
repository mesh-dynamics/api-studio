const gcbrowseConstants = {
    REQUEST_BEGIN: "gcbrowse/REQUEST_BEGIN",
    REQUEST_SUCCESS: "gcbrowse/REQUEST_SUCCESS",
    REQUEST_FAILURE: "gcbrowse/REQUEST_FAILURE",

    LOAD_GOLDENS: "gcbrowse/LOAD_GOLDENS",
    LOAD_USER_GOLDENS: "gcbrowse/LOAD_USER_GOLDENS",
    CLEAR_SELECTED_ITEM: "gcbrowse/CLEAR_SELECTED_ITEM",
    UPDATE_SELECTED_ITEM: "gcbrowse/UPDATE_SELECTED_ITEM",

    SET_MESSAGE: "gcbrowse/SET_MESSAGE",
    CLEAR_MESSAGE: "gcbrowse/CLEAR_MESSAGE",
};

const defaultCollectionItem = {
    app: '',
    archived: false,
    branch: '',
    codeVersion: '',
    collec: '',
    collectionUpdOpSetId: '',
    comment: '',
    cust: '',
    dynamicInjectionConfigVersion: '',
    gitCommitId: '',
    id: '',
    instance: '',
    jarPath: '',
    label: '',
    name: '',
    prntRcrdngId: '',
    recordingType: '',
    rootRcrdngId: '',
    runId: '',
    status: '',
    tags: [],
    templateUpdOpSetId: '',
    templateVer: '',
    timestmp: 0,
    userId: '',
    apiTraces: []
};

export {
    gcbrowseConstants,
    defaultCollectionItem
}