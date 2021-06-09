/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.cache;

import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;

import com.cube.exception.CacheException;

/*
 * Created by IntelliJ IDEA.
 * Date: 20/05/20
 */
public interface TemplateCache {
    CompareTemplate fetchCompareTemplate(TemplateKey key) throws CacheException;

    void invalidateKey(TemplateKey key);

    void invalidateAll();
}
