package com.cubeiosample.webservices.rest.jersey;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;

public class ListMoviesCache {
  
  private MovieRentals mv;
  private Config config;
  private int maxSize = 100;
  
  public ListMoviesCache(MovieRentals mvInstance, Config config) {
    mv = mvInstance;
    this.config = config;
    if (this.config.USE_CACHING) {
    	this.maxSize = 0;
    }
  }
  
  LoadingCache<String, JSONArray> movieIdsCache = CacheBuilder.newBuilder()
      .maximumSize(maxSize)
      .expireAfterAccess(6000, TimeUnit.SECONDS)
      .build(
              new CacheLoader<String, JSONArray>() {
                  public JSONArray load(String filmNameOrKeywordForRequest) {
                         final JSONArray toDo = mv.listMovies(filmNameOrKeywordForRequest);   
                         return toDo;
                  }
              }
      );

  
  JSONArray getMovieList(String filmName) throws ExecutionException {
    final JSONArray movieList = movieIdsCache.get(filmName);
    return movieList; 
  }
}
