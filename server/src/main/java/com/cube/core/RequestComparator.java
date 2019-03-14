/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.core;

import java.util.List;

import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.dao.Request;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-13
 * @author Prasad M D
 */
public interface RequestComparator {

    /**
     * @param lhs
     * @param rhs
     * @return
     */
    MatchType compare(Request lhs, Request rhs);


    ComparisonType getCTreqid();

    List<PathCT> getCTMeta();


    ComparisonType getCTbody();

    ComparisonType getCTcollection();

    ComparisonType getCTtimestamp();

    ComparisonType getCTrrtype();

    ComparisonType getCTcustomerid();

    ComparisonType getCTapp();

    List<PathCT> getCTHdrs();

    ComparisonType getCTpath();

    ComparisonType getCTmethod();

    List<PathCT> getCTQparams();

    List<PathCT> getCTFparams();

    static class PathCT {
        public final String path;
        public final ComparisonType ct;

        public PathCT(String path, ComparisonType ct) {
            this.path = path;
            this.ct = ct;
        }
    }

}
