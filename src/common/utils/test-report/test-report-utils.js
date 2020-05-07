import _ from 'lodash';

const generatePathTableData = (processedTimelineData, replayId) => {
    // split the replay results
    // take the first as the current replay result
    const currentReplayResult = processedTimelineData.shift();

    if (currentReplayResult.replayId !== replayId){
        // todo either move to the next one or it's an error
        console.error("replay id of the current replay result doesn't match the input one")
    }

    // rest are previous results
    const previousReplayResults = processedTimelineData;

    // process the previous replay results and distill them into one object with all the path level data
    const processedPreviousReplayResults = processPreviousReplayResults(previousReplayResults);
    
    // calculate 95th quantile for overall results
    // calculate only if the number of replays is >= 15 (let's start with this and see how it goes)
    const minEntriesForGaussian = 15;
    const prev_95_ci_resp_mm = (processedPreviousReplayResults.all_mm_fraction_arr && processedPreviousReplayResults.all_mm_fraction_arr.length >= minEntriesForGaussian) ? calculate95CI(processedPreviousReplayResults.all_mm_fraction_arr) : null;

    // calculate the average for overall results
    const prev_avg_resp_mm = !_.isEmpty(processedPreviousReplayResults.all_mm_fraction_arr) ? _.mean(processedPreviousReplayResults.all_mm_fraction_arr) : null;

    // combine current and previous result data per path
    return { 
        path_results: Object.entries(currentReplayResult.path_results)
            .map(([path, currPathResult]) => {
                const prevPathResult = processedPreviousReplayResults.path_results ? processedPreviousReplayResults.path_results[path] : null;

                return { 
                    path: path, 
                    
                    total: currPathResult.total,
                    curr_resp_mm: currPathResult.response_mismatches,
                    curr_resp_mm_fraction: currPathResult.response_mismatches / currPathResult.total,
                    
                    prev_avg_resp_mm: prevPathResult ? (_.sum(prevPathResult.response_mismatches_arr) /  (prevPathResult.total * prevPathResult.response_mismatches_arr.length)) : null,
                    prev_95_ci_resp_mm: (prevPathResult && prevPathResult.response_mismatches_arr.length >= minEntriesForGaussian) ? (calculate95CI(prevPathResult.response_mismatches_arr.map((v) => (v / prevPathResult.total)))) : null,
                    
                    prev_path_results_count: prevPathResult ? prevPathResult.response_mismatches_arr.length : 0,
                }
            }),
            prev_95_ci_resp_mm: prev_95_ci_resp_mm,
            prev_avg_resp_mm: prev_avg_resp_mm,
    }
}

// calculate the 95% confidence interval (mu + 2 sigma) of an array (capped at 1)
const calculate95CI = (arr) => {
    const mu = _.mean(arr);
    const diffArr = arr.map(a => (a - mu) ** 2);
    const sigma = arr.length!==1 ? Math.sqrt(_.sum(diffArr) / (arr.length - 1)) : 0; // 0 for single value

    return Math.min(mu + 2 * sigma, 1); // capping at 1
}

const processPreviousReplayResults = (previousReplayResults) => {
    return reduceTimelineData(previousReplayResults);
}

// for each replay, and each path in it, transform and compute the response mismatches, total responses; and reduce to one object per replay
const processTimelineData = (timelineres, paths) => {
    return timelineres.timelineResults
        .filter(replay => Object.keys(replay).length !== 0) // remove any empty replay objects (todo is this ok?)
        .map((tr) => {
            let path_results = tr.results
                .filter(r => _.isEmpty(paths) || paths.includes(r.path)) // if path list is empty, select all paths otherwise filter out 
                .map((r) => { // for each path, compute the response mismatches and total responses
                    return {
                        path: r.path,
                        response_mismatches: r.respnotmatched + r.respmatchexception,
                        total: r.respmatched + r.respnotmatched + r.resppartiallymatched + r.respmatchexception,
                    }
                })
                .reduce((acc, r) => { // reduce all path stats above into one object having a map of path -> stats
                        let accrpath = acc[r.path] || {
                            response_mismatches: 0,
                            total: 0,
                            mismatch_fraction: 0,
                        };
                        // assigning the path data to acc path data
                        accrpath.response_mismatches += r.response_mismatches;
                        accrpath.total += r.total;
                        accrpath.mismatch_fraction = accrpath.response_mismatches / accrpath.total;

                        acc[r.path] = accrpath;
                        return acc;
                    },
                {});

        let all_total = Object.entries(path_results).map(([path, pathData]) => {
            return pathData.total;
        });

        let all_resp_mm = Object.entries(path_results).map(([path, pathData]) => {
            return pathData.response_mismatches;
        });

        return {
            path_results: path_results,
            replayId: tr.replayId,
            all_total: _.sum(all_total),
            all_resp_mm: _.sum(all_resp_mm),
        }
    });
}

// reduce the path stats across all replays into one object with the path error values in an array
const reduceTimelineData = (processedTimelineData) => {
    return processedTimelineData.reduce((acc, replayObject) => { // reduce over replay objects
        // todo the orignal object seems to get modified
        if ('path_results' in acc === false) { 
            // to initialize the accumulator
            acc.path_results = {...replayObject.path_results};
            // array of all mismatch fractions across replays
            acc.all_mm_fraction_arr = [];
        }

        // path_result object from the accumulator
        let accPathRes = acc.path_results;
    
        // collect the mm fractions of all replays, used to calculate the stats later
        acc.all_mm_fraction_arr = acc.all_mm_fraction_arr.concat(replayObject.all_resp_mm/replayObject.all_total);

        // iterate over paths and corresp stats; and add the data to the accumulator
        let en = Object.entries(replayObject.path_results);
        en.forEach(([path, pathStats]) => {
            let acp = accPathRes[path];
            if (!acp.response_mismatches_arr) acp.response_mismatches_arr = [];
            acp.response_mismatches_arr = acp.response_mismatches_arr.concat(pathStats.response_mismatches);
            accPathRes[path] = acp;
        })
        acc.path_results = accPathRes;
        return acc;
    }, {})
}

export {processTimelineData, generatePathTableData}