package io.md.cache;

import java.util.Map;

public interface MDCache {

	/*
	Name of the Cache
	*/
	public String getName();

	/*
		Clean the cache with Given cache specific meta
	 */
	public long clean(Map<String,?> keyMeta);
}
