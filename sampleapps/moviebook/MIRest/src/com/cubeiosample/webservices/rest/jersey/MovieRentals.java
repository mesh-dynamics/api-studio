package com.cubeiosample.webservices.rest.jersey;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.opentracing.Tracer;


public class MovieRentals {

    private static RestOverSql ros = null;
    private static BookInfo bookInfo = null;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Tracer tracer;
    private final Config config;

    final static Logger LOGGER = Logger.getLogger(MovieRentals.class);

    private final Random random = new Random();


    public MovieRentals(Tracer tracer, Config config) throws ClassNotFoundException {
    		loadDriver();
    		this.tracer = tracer;
    		this.config = config;

	    try {
	    	LOGGER.debug("MV tracer: " + tracer.toString());
	    	ros = new RestOverSql(this.tracer, config);


	    	if (this.config.GET_BOOK_REVIEWS) {
	    		bookInfo = new BookInfo(tracer, config);
	    	}
	    } catch (Exception e) {
	   		LOGGER.error("MovieRentals constructor failed; " + e.toString());
	    }

	    // health check of the ROS
	    try {
	      LOGGER.info(ros.getHealth());
	    } catch (Exception e) {
	      LOGGER.error("health check of RestWrapper over JDBC failed; " + e.toString());
	    }
    }


    public JSONArray listMovies(String filmnameOrKeywordForRequest) {
	    	// TODO: add actor also in the parameter options.
	    	try {
		    	JSONArray films = null;
		    	films = listMovieByName(filmnameOrKeywordForRequest);
	    		if (films == null || films.length() == 0) {
					films = listMoviesByKeyword(filmnameOrKeywordForRequest);
	    		}
				if (films != null && films.length() > 0) {
					if (Config.V2.equalsIgnoreCase(config.VERSION)) {
						for (int i = 0; i < films.length(); ++i) {
							JSONObject film = films.getJSONObject(i);
							JSONArray nearByStores = this.findAvailableStores(film.getInt("film_id"));
							film.put("nearby_stores", nearByStores);
						}
					}
					return films;
				}
	    	} catch (Exception e) {
	    		LOGGER.error("Couldn't list movies; " + e.toString());
	    	}

	    	return new JSONArray("[{\"couldn't list movies\"}]");
    }


    public JSONArray listMovieByName(String filmname) {
      // Query with filmname
      LOGGER.debug("filmname:" + filmname);
      // String query = "select film_id, title from film where title = ?";
      String query = "select film.film_id as film_id, film.title as title, group_concat(actor_film_count.first_name) as actors_firstnames, group_concat(actor_film_count.last_name) as actors_lastnames, group_concat(actor_film_count.film_count) as film_counts "
      		  + "from film, film_actor, actor_film_count "
    		  + " where film.film_id = film_actor.film_id and film_actor.actor_id = actor_film_count.actor_id "
    		  + " and title = ?"
    		  + " group by film.film_id, film.title";

      JSONArray params = new JSONArray();
      RestOverSql.addStringParam(params, filmname);
      JSONArray films = null;
      films = ros.executeQuery(query, params);

      processActorNamesForDisplay(films);
      addTimestamp(films);
      if (config.GET_BOOK_REVIEWS) {
          enhanceFilmsWithReviews(films);
      }
      LOGGER.debug(String.format("Movie and book info list: %s", films.toString()));
      return films;
    }

    private static final String TIMESTAMP = "timestamp";
    private static final String FIRST_NAMES = "actors_firstnames";
    private static final String LAST_NAMES = "actors_lastnames";
    private static final String FILM_COUNTS = "film_counts";
    private static final String DISPLAY_ACTORS = "display_actors";
    private void addTimestamp(JSONArray films){
        for (int i = 0; i < films.length(); ++i) {
            JSONObject film = films.getJSONObject(i);
            film.put(TIMESTAMP , System.nanoTime());
        }
    }

    private String arrayifyOrRemoveRandomly(JSONObject jsonObject , String fieldName  , Random forShuffle, boolean alwaysHide) {
		String value = jsonObject.getString(fieldName);

		jsonObject.remove(fieldName);

		double valueFate = 1;

		if (config.ADD_FIELD_RANDOM) {
			valueFate = random.nextDouble();
		}

		String[] valueArr = value.split(",");
		LOGGER.debug("Name value " + value);
		List<String> valueList = Arrays.asList(valueArr);

		LOGGER.debug("Values before shuffle :" + valueList);

		if (config.SHUFFLE_VALUES) {
			LOGGER.debug("Values to be shuffled");
			Collections.shuffle(valueList, forShuffle);
		}

		LOGGER.debug("Values after shuffle" + valueList);

		if ( valueFate >= 0.5 ) {
			if (!alwaysHide)
				jsonObject.put(fieldName, valueList);
		}

		return value;
	}

	private String arrayifyToNumbers(JSONObject jsonObject , String fieldName , Random forShuffle) {
    	String value = jsonObject.getString(fieldName);
		String[] valueArr = value.split(",");
		List<String> valueList = Arrays.asList(valueArr);
		jsonObject.remove(fieldName);
		if (config.CONCAT_BUG) {
				if (config.SHUFFLE_VALUES)
		    	Collections.shuffle(valueList , forShuffle);
		    jsonObject.put(fieldName , valueList);
		} else {
		    List<Integer> valuesAsIntegerList =
                    Arrays.stream(valueArr).map(Integer::valueOf).collect(Collectors.toList());
		    jsonObject.put(fieldName , valuesAsIntegerList);
        }
    	return value;
	}

    private void processActorNamesForDisplay(JSONArray films) {
    	LOGGER.debug("Entered into processActorNamesForDisplay");
    	for (int i = 0; i < films.length(); ++i) {
    		JSONObject film = films.getJSONObject(i);
			long seed = System.nanoTime();
			// having the same seed for shuffling all the arrays in the same order
    		String firstNames  = arrayifyOrRemoveRandomly(film, FIRST_NAMES , new Random(seed), config.ALWAYS_HIDE_FIRST_NAME);
    		String lastNames = arrayifyOrRemoveRandomly(film, LAST_NAMES , new Random(seed), config.ALWAYS_HIDE_LAST_NAME);
    		String filmCounts = arrayifyToNumbers(film, FILM_COUNTS , new Random(seed));
    		List<String> displayActors = displayActors(firstNames, lastNames, filmCounts);
    		JSONArray array = new JSONArray();
    		displayActors.forEach(actor-> array.put(actor));
    		film.put(DISPLAY_ACTORS, array);
    	}
    }

    private List<String> displayActors(String firstNames, String lastNames, String filmCounts) {
    	LOGGER.debug(String.format("finding display actors for %s, %s, %s", firstNames, lastNames, filmCounts));
    	int[] counts = Arrays.stream(filmCounts.split(",")).mapToInt(Integer::parseInt).toArray();
		List<String> result = new ArrayList<>();
    	if (counts.length == 0) {
    		return result;
    	}

    	Integer numOfActorsToDisplay = (config.CONCAT_BUG) ? 3 : 4;
    	boolean displayNameLastFirst = (config.CONCAT_BUG);
    	List<Integer> impActorIndexes = maxKIndexes(counts, numOfActorsToDisplay);
    	String[] fNamesArr = firstNames.split(",");
    	String[] lNamesArr = lastNames.split(",");

    	for (int i = 0; i < impActorIndexes.size(); ++i) {
    		if (displayNameLastFirst) {
    			result.add(buggyAppend(lNamesArr[impActorIndexes.get(i)], fNamesArr[impActorIndexes.get(i)], ",")/* + "; "*/);
    		} else {
    			result.add(buggyAppend(fNamesArr[impActorIndexes.get(i)], lNamesArr[impActorIndexes.get(i)], " ") /*+ "; "*/);
    		}
    	}
    	return result;
    }

    private String buggyAppend(String name1, String name2, String separator) {
    	if (config.CONCAT_BUG) {
    		// introduce the concat bug only occasionally so it stresses comprehensiveness.
    		// Narrative: Firstnames in the database have trailing spaces and hence this bug only appears occasionally.
    		if (name1.toLowerCase().startsWith("a") || name1.toLowerCase().startsWith("p") || name1.toLowerCase().startsWith("s")) {
    			return name1 + name2;
    		}
    		return name1 + separator + name2;
    	} else {
    		return name1 + separator + name2;
    	}
    }

    // Always choose k indexes.
    // If multiple values in counts are equal to the maxKThreshold then we choose randomly among them.
    private List<Integer> maxKIndexes(int[] counts, int k) {
    	List<Integer> indexes = new ArrayList<>();
       	List<Integer> indexesToChooseRandomly = new ArrayList<>();
        int maxKValue = maxKThreshold(Arrays.copyOf(counts, counts.length), k);
    	for (int i = 0; i < counts.length; ++i) {
    		if (counts[i] > maxKValue) {
    			indexes.add(i);
    		} else if (counts[i] == maxKValue) {
    			// add it to the set from which we will pick randomly.
    			indexesToChooseRandomly.add(i);
    		}
    	}
    	// num to choose randomly.
    	int num_iterations = 0;
    	while (k > indexes.size() && indexesToChooseRandomly.size() > 0 && num_iterations < 1000) {
    		// pick randomly.
			if (Math.random() > 0.5) {
				indexes.add(indexesToChooseRandomly.get(num_iterations % indexesToChooseRandomly.size()));
				indexesToChooseRandomly.remove(num_iterations % indexesToChooseRandomly.size());
			} else {
				num_iterations++;
			}
    	}

    	return indexes;
    }

    private int maxKThreshold(int[] countsArr, int k) {
    	// assume k > 0
    	if (k == 0) {
    		return Integer.MAX_VALUE;
    	}
    	Arrays.sort(countsArr);
    	int maxKValue = countsArr[Math.max(0, countsArr.length-k)];
    	LOGGER.debug(String.format("maxKValue for %s is %d", countsArr.toString(), maxKValue));
    	return maxKValue;
    }

    private void enhanceFilmsWithReviews(JSONArray films) {
        // TODO: avoid modifying the film object. Instead, attach another reviews object
        // But that requires the client to change. Hence keeping the change by adding another column
        for (int i = 0; i < films.length(); ++i) {
            JSONObject film = films.getJSONObject(i);
            JSONObject binfo = bookInfo.getBookInfo(film.getString("title"), film.getInt("film_id"));
            film.put("book_info", binfo);
        }
    }


    public JSONArray listMoviesByKeyword(String keyword) {
      String query = "select id, title from film where title like %?%";
      JSONArray params = new JSONArray();
      RestOverSql.addStringParam(params, keyword);
      JSONArray films = null;
      LOGGER.debug("params array:" + params.toString());
      return ros.executeQuery(query, params);
    }


    public JSONArray findAvailableStores(int filmId) throws SQLException, JSONException {
	    	try {
		    	String storesQuery = "select distinct store_id from inventory "
		    			+ " where film_id = ? and "
		    			+ " inventory_id not in (select inventory_id from rental where return_date is null)";
		    	JSONArray params = new JSONArray();
		    	RestOverSql.addIntegerParam(params, filmId);
	    	    if (ros == null) {
	    	    	LOGGER.debug("Creating ROS since it is null");
	    	    	ros = new RestOverSql(tracer, config);
		    	}
		    	// else
		    	return ros.executeQuery(storesQuery, params);
	    	} catch (Exception e) {
	    		LOGGER.error("FindAvailableStores failed on filmId=" + filmId + "; " + e.toString());
	    	}
	    	return null;
    }


    public JSONArray findDues(int userId) throws SQLException, JSONException {
        String duesQuery = "select * from rental where return_date is null and customer_id = ?";
	    JSONArray params = new JSONArray();
	    RestOverSql.addIntegerParam(params, userId);
        return ros.executeQuery(duesQuery, params);
    }


    public JSONObject rentMovie(int film_id, int store_id, int duration, int customer_id, int staff_id) throws SQLException, JSONException {
    	// Update rental table
    	int inventoryId = getInventoryId(film_id, store_id);
    	JSONObject result = new JSONObject();
    	result.put("inventory_id", inventoryId);
    	if (inventoryId < 0) {
    		return result;
    	}

    	double rentalRate = getRentalRate(film_id);

    	JSONObject rentResult = updateRental(inventoryId, customer_id, staff_id);
    	int numUpdates = rentResult.getInt("num_updates");
    	result.put("num_updates", numUpdates);
    	if ( numUpdates < 0) {
    		return result;
    	}
    	result.put("rent", rentalRate*duration);
    	LOGGER.debug("rent movie result returning: " + result.toString());
    	return result;
    }


    public JSONObject returnMovie(int inventoryId, int customerId, int staffId, double rent) {
	    	return returnRental(inventoryId, customerId, staffId, rent);
    }

	public int getCustomerId(String userName) throws Exception {
		String query = "select customer_id from customer where email = ?";
		JSONArray params = new JSONArray();
		RestOverSql.addStringParam(params, userName);
		JSONArray rs =  ros.executeQuery(query, params);
		if (rs == null || rs.length() < 1) {
			throw new Exception(String.format("No Customer found for the userName:%s", userName));
		}
		JSONObject json = rs.getJSONObject(0);
		return json.getInt("customer_id");
	}

	public JSONObject getGenreGroupById(Integer id) throws Exception {
		String genreGroupQuery = "select * from genre_group "
				+ " where genre_group_id = ?";
		JSONArray params = new JSONArray();
		RestOverSql.addIntegerParam(params, id);
		JSONArray rs =  ros.executeQuery(genreGroupQuery, params);
		if (rs == null || rs.length() < 1) {
			throw new Exception(String.format("No genre group found for the id:%s", id));
		}
		return  rs.getJSONObject(0);
	}

	public JSONObject getGenreGroupForCustomerByName(String name, int customerId) throws Exception {
		String genreGroupQuery = "select * from genre_group "
				+ " where name = ? and customer_id = ?";
		JSONArray params = new JSONArray();
		RestOverSql.addStringParam(params, name);
		RestOverSql.addIntegerParam(params, customerId);
		JSONArray rs =  ros.executeQuery(genreGroupQuery, params);
		if (rs == null || rs.length() < 1) {
			LOGGER.error(String.format("No genre group found for the name:%s", name));
			return null;
		}
		return  rs.getJSONObject(0);
	}

	public JSONObject updateGenreGroupName(String name , int id) {
		String query = "Update genre_group SET name=? where genre_group_id = ?";
		JSONArray params = new JSONArray();
		RestOverSql.addStringParam(params, name);
		RestOverSql.addIntegerParam(params, id);
		return ros.executeUpdate(query, params);
	}

	public JSONObject createGenreGroup(String name, int customerId) {
		String query = "INSERT INTO genre_group (name, customer_id) VALUES (?, ?)";
		JSONArray params = new JSONArray();
		RestOverSql.addStringParam(params, name);
		RestOverSql.addIntegerParam(params, customerId);
		return ros.executeUpdate(query, params);
	}

	public JSONArray getAllCategories() {
		String query = "Select category_id, name from category";
		JSONArray params = new JSONArray();
		Map<String, Integer>  categoryIdValues = new HashMap<>();
		return ros.executeQuery(query, params);
	}

	public JSONArray getCategoriesByNames(List<String> categories) {
		String query = "Select category_id, name from category where name In (";
		JSONArray params = new JSONArray();
		for(int i=0; i< categories.size() && i < 60; i++) {
			query = query + "?";
			if (i < categories.size()-1)
				query = query + ",";
			if(i == categories.size()-1)
				query = query + ")";
			RestOverSql.addStringParam(params, categories.get(i));
		}
		return ros.executeQuery(query, params);
	}

	private Map<String, Integer> categoryNameIdMap(List<String> categories) {
		Map<String, Integer>  nameIdMap = new HashMap<>();
		JSONArray rs = getCategoriesByNames(categories);
		for(Object obj: rs) {
			JSONObject jsonObject = (JSONObject)obj;
			nameIdMap.put(jsonObject.getString("name"), jsonObject.getInt("category_id"));
		}
		return  nameIdMap;
	}

	public JSONObject deleteGenreGroupCategoryMapping(int genreGroupId) {
		String genreGroupQuery = "delete from genre_group_category "
				+ " where genre_group_id = ?";
		JSONArray params = new JSONArray();
		RestOverSql.addIntegerParam(params, genreGroupId);
		return ros.executeUpdate(genreGroupQuery, params);
	}

	public JSONObject deleteGenreGroup(int genreGroupId) {
		String genreGroupQuery = "delete from genre_group where genre_group_id= ?";
		JSONArray params = new JSONArray();
		RestOverSql.addIntegerParam(params, genreGroupId);
		return ros.executeUpdate(genreGroupQuery, params);
	}

	public boolean genre_group_category_mapping(List<String> categories, int genreGroupId) {
		try {

			deleteGenreGroupCategoryMapping(genreGroupId);
			Map<String, Integer>  categoryNameIdMap = categoryNameIdMap(categories);
			String query = "INSERT INTO genre_group_category (genre_group_id, category_id) VALUES ";
			JSONArray params = new JSONArray();
			for (int i=0; i<categories.size();i++) {
				if (categoryNameIdMap.containsKey(categories.get(i))) {
					query = query + "(?, ?)";
					if (i < categories.size() - 1)
						query = query + ",";
					RestOverSql.addIntegerParam(params, genreGroupId);
					RestOverSql.addIntegerParam(params, categoryNameIdMap.get(categories.get(i)));
				}
			}
			LOGGER.debug(query + "; " + params.toString());
			ros.executeUpdate(query, params);
			return true;
		}catch (Exception e) {
			LOGGER.error("Couldn't update genre group category mapping, error: " + e.toString());
			throw e;
		}
	}

	public Collection<JSONObject> getALlGenreGroupsForCustomer(String email) {
    	String query = "select D3.name as categoryName, D2.category_id, D1.name, D1.genre_group_id from category D3 "
					+ "left join (genre_group_category D2 join "
					+ "(genre_group D1 join customer D0 "
					+ "on D1.customer_id=D0.customer_id) "
					+ "on D2.genre_group_id=D1.genre_group_id) "
					+ "on D3.category_id=D2.category_id where D0.email=?";
		JSONArray params = new JSONArray();
		RestOverSql.addStringParam(params, email);
		JSONArray rs =  ros.executeQuery(query, params);
		JSONArray response = new JSONArray();
		Map<Integer, JSONObject> jsonMap = new HashMap<>();
		for(Object obj:rs) {
			JSONObject json = (JSONObject)obj;

			int genre_group_id = json.getInt("genre_group_id");
			String name = json.getString("name");
			String categoryName = json.getString("categoryName");
			int category_id = json.getInt("category_id");

			JSONObject categoryObj = new JSONObject();
			categoryObj.put("category_id", category_id);
			categoryObj.put("name", categoryName);

			JSONArray catel;
			JSONObject value;
			if(jsonMap.containsKey(genre_group_id)) {
				value = jsonMap.get(genre_group_id);
				catel = (JSONArray)value.get("categories");
			} else {
				value = new JSONObject();
				value.put("genre_group_id", genre_group_id);
				value.put("name", name);
				catel = new JSONArray();
			}
			catel.put(categoryObj);
			value.put("categories", catel);
			jsonMap.put(genre_group_id, value);
		}
		return jsonMap.values();
	}

	public JSONArray getAllMovies() {
    	String query = "select * from film";
    	JSONArray params = new JSONArray();
    	return ros.executeQuery(query, params);
	}

	public JSONArray getMoviesForGroup(int genreGroupId) {
    	String query = "Select * from film where film_id  IN (select film_id from film_category where category_id IN(select category_id from genre_group_category where genre_group_id =?))";
    	JSONArray params = new JSONArray();
    	RestOverSql.addIntegerParam(params, genreGroupId);
    	return ros.executeQuery(query, params);
	}


//    public boolean IsBookBased(String title) {
//    	return ExistsFilm(title);
//    }


    // Sales and store performance analysis
    /*public JSONArray getSalesByStore(String store_name) throws SQLException, JSONException {
        String query = "select * from sales_by_store where store = '" + store_name + "'";
        	LOGGER.debug(query);
        JSONArray rs = ros.executeQuery(query);
        return rs;
    }*/


    private static void loadDriver() throws ClassNotFoundException {
        // This will load the MySQL driver, each DB has its own driver
        Class.forName("com.mysql.jdbc.Driver");
    }


    public JSONArray rentFilmsBulk(JSONArray filmArray, int storeId, int duration, int customerId, int staffId) throws JSONException, SQLException {
	    	Map<String, Integer> films = new HashMap<>();
	    	try {
		    	for (int i = 0; i < filmArray.length(); ++i) {
		    		String film = filmArray.getString(i);
		    		films.put(film, i);  // i is not used any where. Need to create the hashmap.
		    	}
	    	} catch (Exception e) {
	    		LOGGER.error("Error while creating a hashmap; " + e.toString());
	    	}

	    	// iteration
	    	JSONArray rentals = new JSONArray();
	    	for (Map.Entry<String, Integer> entry : films.entrySet()) {
	    	  JSONObject obj = this.rentMovie(entry.getValue(), storeId, duration, customerId, staffId);
	    		rentals.put(obj);
	    	}
	    	LOGGER.info(rentals.toString());
	    	return rentals;
    }


    private int getInventoryId(int film_id, int store_id) {
	    	int inventory_id = -1;
	    	try {
	    		JSONArray rs = null;
    			String inventoryQuery = "select inventory_id from inventory "
		    			+ " where film_id = ? and store_id = ? and "
		    			+ " inventory_id not in (select inventory_id from rental where return_date is null) limit 1";
		    	JSONArray params = new JSONArray();
		    	RestOverSql.addIntegerParam(params, film_id);
		    	RestOverSql.addIntegerParam(params, store_id);
		    	rs = ros.executeQuery(inventoryQuery, params);
	    		if (rs == null || rs.length() < 1) {
		    		return -1;
		    	}
	    		LOGGER.debug("getinventoryid: " + rs.toString());
		    	return rs.getJSONObject(0).getInt("inventory_id");
	    	} catch (Exception sqlException) {
	    		LOGGER.error("Couldn't prepare rental update stmt: " + sqlException.toString());
	    	}
	    	return inventory_id;
    }


    private double getRentalRate(int film_id) throws SQLException, JSONException {
      String query = "select rental_rate from film where film_id = ?";
      JSONArray params = new JSONArray();
      RestOverSql.addIntegerParam(params, film_id);
      JSONArray rs = null;
      rs = ros.executeQuery(query, params);
      if (rs.length() < 1) {
        return -1;
  	  }
      return rs.getJSONObject(0).getDouble("rental_rate");
    }


    private JSONObject updateRental(int inventory_id, int customer_id, int staff_id) throws SQLException, JSONException {
        String dateString = format.format(new Date());
      	String rentalUpdateQuery = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                  + " VALUES (?, ?, ?, ?)";
      	JSONArray params = new JSONArray();
      	RestOverSql.addIntegerParam(params, inventory_id);
      	RestOverSql.addIntegerParam(params, customer_id);
      	RestOverSql.addIntegerParam(params, staff_id);
      	RestOverSql.addStringParam(params, dateString);
      	LOGGER.debug(rentalUpdateQuery + "; " + params.toString());
    	return ros.executeUpdate(rentalUpdateQuery, params);
    }


    private int getRentalIdForReturn(int inventoryId, int customerId, int staffId) {
      try {
        String rentalIdForReturnQuery = "SELECT rental_id from rental WHERE inventory_id = ? and customer_id = ? and staff_id = ? and return_date is null";
        JSONArray params = new JSONArray();
        RestOverSql.addIntegerParam(params, inventoryId);
        RestOverSql.addIntegerParam(params, customerId);
        RestOverSql.addIntegerParam(params, staffId);
        JSONArray rs = null;
        rs = ros.executeQuery(rentalIdForReturnQuery, params);
        return rs.getJSONObject(0).getInt("rental_id");
      } catch (Exception e) {
        LOGGER.info("Couldn't find rental_id for [" + inventoryId + ", " + customerId + ", " + staffId + "]");
        return -1;
      }
    }


    private JSONObject returnRental(int inventoryId, int customerId, int staffId, double amount) {
      JSONObject result = new JSONObject();
      String dateString = format.format(new Date());
      int rentalId = getRentalIdForReturn(inventoryId, customerId, staffId);
      result.put("rental_id", rentalId);
      if (rentalId == -1) {
        return result;
      }

      String rentalReturnQuery = "UPDATE rental SET return_date = ? WHERE rental_id = ?";
      JSONArray params1 = new JSONArray();
      RestOverSql.addStringParam(params1, dateString);
      RestOverSql.addIntegerParam(params1, rentalId);
      JSONObject returnUpdate = null;
      returnUpdate = ros.executeUpdate(rentalReturnQuery, params1);
      int returnUpdates = returnUpdate.getInt("num_updates");
      result.put("return_updates", returnUpdates);
      if (returnUpdates == -1) {
        return result;  // failure
      }

      String paymentQuery = "INSERT INTO payment (customer_id, staff_id, rental_id, amount, payment_date) VALUES (?, ?, ?, ?, ?)";
      JSONArray params2 = new JSONArray();
      RestOverSql.addIntegerParam(params2, customerId);
      RestOverSql.addIntegerParam(params2, staffId);
      RestOverSql.addIntegerParam(params2, rentalId);
      RestOverSql.addDoubleParam(params2, amount);
      RestOverSql.addStringParam(params2, dateString);
      JSONObject paymentUpdate = null;
      paymentUpdate = ros.executeUpdate(paymentQuery, params2);
      result.put("payment_updates", paymentUpdate.getInt("num_updates"));
      return result;
    }

	public int updateInventory(int number) {
		try {
			JSONArray rs = null;
			String inventoryQuery = "SELECT DISTINCT film_id, store_id from inventory";
			JSONArray params = new JSONArray();
			rs = ros.executeQuery(inventoryQuery, params);
			if (rs == null || rs.length() < 1) {
				return -1;
			}
			for(Object obj: rs) {
				JSONObject jsonObject = (JSONObject)obj;
				int filmId = jsonObject.getInt("film_id");
				int storeId = jsonObject.getInt("store_id");
				String inventoryInsertQuery = "INSERT INTO inventory (film_id, store_id) VALUES ";
				params = new JSONArray();
				for (int i=0; i<number;i++) {
					inventoryInsertQuery = inventoryInsertQuery + "(?, ?)";
					if (i < number-1)
						inventoryInsertQuery = inventoryInsertQuery + ",";
					RestOverSql.addIntegerParam(params, filmId);
					RestOverSql.addIntegerParam(params, storeId);
				}
				LOGGER.debug(inventoryInsertQuery + "; " + params.toString());
				ros.executeUpdate(inventoryInsertQuery, params);
			}
			return rs.length();
		} catch (Exception sqlException) {
			LOGGER.error("Couldn't prepare  update inventory stmt: " + sqlException.toString());
		}
		return -1;
	}

	public int deleteRental() {
		String inventoryQuery = "DELETE from rental";
		JSONArray params = new JSONArray();
		JSONObject rs = ros.executeUpdate(inventoryQuery, params);
		return rs.getInt("num_updates");
	}

	public boolean validateUserAndPassword(String userName, String password) {
		try {
			String storesQuery = "select * from customer "
					+ " where email = ? and password = ?";
			JSONArray params = new JSONArray();
			RestOverSql.addStringParam(params, userName);
			RestOverSql.addIntegerParam(params, encodePassword(password));
			JSONArray rs = ros.executeQuery(storesQuery, params);
			if (rs == null || rs.length() < 1) {
				return false;
			}
			return true;
		} catch (Exception e) {
			LOGGER.error("No user found for  user=" + userName + "; " + e.toString());
		}
		return false;
	}

	private int encodePassword(String password) {
    	int hash = 23;
    	for(int i=0; i<password.length(); i++) {
    		hash = hash * 23 + password.charAt(i);
			}
    	return hash;
	}
    /*
    
      private int GetFilmId(String filmName) throws SQLException {
    	String query = "select film_id from film where title = ?";  // TODO: escape sql strings
    	
    	PreparedStatement stmt = connPool.getConnection().prepareStatement(query);
    	stmt.setString(1, filmName);
    	ResultSet rs = stmt.executeQuery();
    	int filmId = -1;
    	while (rs.next()) {
    		filmId = rs.getInt(1);
    	}
    	return filmId;
    }
    
    private void PrepareInventoryStatement() {
    	try {
	    	String inventoryQuery = "select inventory_id from inventory "
	    			+ " where film_id = ? and store_id = ? and "
	    			+ " inventory_id not in (select inventory_id from rental where return_date is null) limit 1";
	    	inventory_stmt = connPool.getConnection().prepareStatement(inventoryQuery);
    	} catch (Exception sqlException) {
    		LOGGER.error("Couldn't prepare inventory stmt: " + sqlException.toString());
    	}
    }

    
    
    
    
    private void PrepareRentalUpdateStmt() {
    	try {
    		String rentalUpdateQuery = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                    + " VALUES (?, ?, ?, ?)";
    		rental_update_stmt = connPool.getConnection().prepareStatement(rentalUpdateQuery);
    	} catch (Exception sqlException) {
    		LOGGER.error("Couldn't prepare rental update stmt: " + sqlException.toString());
    	}
    }
    
    
    private int UpdateRentalPrepStmt(int inventory_id, int customer_id, int staff_id) {
    	String dateString = format.format(new Date());
    	try {
	    	rental_update_stmt.setInt(1, inventory_id);
	    	rental_update_stmt.setInt(2, customer_id);
	    	rental_update_stmt.setInt(3, staff_id);
	    	rental_update_stmt.setString(4, dateString);
	    	return rental_update_stmt.executeUpdate();
    	} catch (Exception sqlException) {
    		LOGGER.error("Couldn't prepare rental update stmt: " + sqlException.toString());
    	}
    	return -1;
    }

    // Copied from the stackoverflow post
    // https://stackoverflow.com/questions/6514876/most-efficient-conversion-of-resultset-to-json
    private JSONArray ConvertResultSetToJson(ResultSet rs) throws JSONException {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();
            String[] columnNames = new String[numColumns];
            int[] columnTypes = new int[numColumns];

            for (int i = 0; i < columnNames.length; i++) {
                columnNames[i] = rsmd.getColumnLabel(i + 1);
                columnTypes[i] = rsmd.getColumnType(i + 1);
            }

            JSONArray rows = new JSONArray();
            while(rs.next()) {
                JSONObject obj = new JSONObject();
                // resultset index starts from 1
                for (int i = 1; i <= numColumns; i++) {
                    String column_name = rsmd.getColumnName(i);
                    obj.put(column_name, rs.getObject(i));
                }
                rows.put(obj);
            }
            return rows;
        } catch (SQLException e) {
            // log e
        	LOGGER.error("couldn't convert result to json: " + e.toString());
            return null;
        }
    }
    */
}
