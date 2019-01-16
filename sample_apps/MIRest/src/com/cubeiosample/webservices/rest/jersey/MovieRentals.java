package com.cubeiosample.webservices.rest.jersey;

import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

//import org.json.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class MovieRentals {

    private ConnectionPool jdbcPool = null;
    private static RestOverSql ros = null;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static boolean USE_PREPARED_STMTS = true;    
    private static boolean USE_KUBE = false;
    private static boolean USE_JDBC_SERVICE = true;
    private static String MYSQL_HOST = "localhost";
    private static String MYSQL_PORT = "3306";
    private static String MYSQL_USERNAME = "cube";
    private static String MYSQL_PWD = "cubeio";
    
    final static Logger LOGGER = Logger.getLogger(MovieRentals.class);
    
    public MovieRentals() throws ClassNotFoundException {
    		LoadDriver();
    	
	    // TODO: make a separate database query service.
    		configureUseKube();
	    jdbcPool = new ConnectionPool();
	    try {
	      if (USE_JDBC_SERVICE) {
	        ros = new RestOverSql();
	      } else {
	        String uri = "jdbc:mysql://" + baseUri() + "/sakila";
  	    		  LOGGER.info("mysql uri: " + uri);
          jdbcPool.setUpPool(uri, userName(), passwd());
          LOGGER.info(jdbcPool.getPoolStatus());
	      }
	    } catch (Exception e) {
	    		LOGGER.error("connection pool creation failed; " + e.toString());
	    }
	    
	    // health check of the ROS
	    try {
	      LOGGER.info(ros.getHealth());
	    } catch (Exception e) {
	      LOGGER.error("health check of RestWrapper over JDBC failed; " + e.toString());
	    }
    }
    
    private void configureUseKube() {
    		String useKube = System.getenv("USE_KUBE");
    		if (useKube != null && useKube.equalsIgnoreCase("true")) {
    			USE_KUBE = true;
    		}
    		LOGGER.debug("use_kube value:" + useKube);
    }
    
    public static String baseUri() {
    		if (USE_KUBE) {
    			// couldn't pass the IP of another pod. But the service has it as <svcname>_SERVICE_HOST
    			//String host = System.getenv(System.getenv("MYSQL_PERMANENT_HOST"));
    		  String host = System.getenv("MYSQL_PERMANENT_HOST");
    			if (host == null) {
    			  LOGGER.error("host has to be specified");
    			}
    			String port = System.getenv("MYSQL_DB_PORT");
    			if (port == null) {
    				port = "3306";
    			}
    			return "jdbc:mysql://" + host + ":" + port + "/sakila";
    		}
    		return "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/sakila";
    }
    
    public static String userName() {
      if (USE_KUBE) {
    		 return System.getenv("MYSQL_DB_USER");
      }
    		return MYSQL_USERNAME;
    }
    
    public static String passwd() {
      if (USE_KUBE) {
    		  return System.getenv("MYSQL_DB_PASSWORD");
      }
    		return MYSQL_PWD;
    }


    public JSONArray ListMovies(String filmNameOrKeyword) {
	    	// TODO: add actor also in the parameter options.
	    	try {
		    	JSONArray films = null;
		    	films = ListMovieByName(filmNameOrKeyword);
		    	if (films != null && films.length() > 0) {
	          return films;
		    	}
		    
		    	films = ListMoviesByKeyword(filmNameOrKeyword);
	    		if (films != null && films.length() > 0) {
	    			return films;
	    		}
	    	} catch (Exception e) {
	    		LOGGER.error("Couldn't list movies; " + e.toString());
	    	}
	    	return new JSONArray("[{\"couldn't list movies\"}]");
    }
    
    
    public JSONArray ListMovieByName(String filmName) {
      if (filmName == null || filmName.isEmpty()) {
        return null;
      }
           
      // query with filmname
      String query = "select film_id, title from film where title = ?";
      JSONArray params = new JSONArray();
      RestOverSql.AddStringParam(params, filmName);
      JSONArray films = null;
      if (USE_JDBC_SERVICE) {
        films = ros.ExecuteQuery(query, params);
      } else {
        films = jdbcPool.ExecuteQuery(query, params);
      }
      return films;
    }
    
    
    public JSONArray ListMoviesByKeyword(String keyword) {
      if (keyword == null || keyword.isEmpty()) {
        return null;
      }
      String query = "select id, title from film where title like %?%";
      JSONArray params = new JSONArray();
      RestOverSql.AddStringParam(params, keyword);
      JSONArray films = null;
      if (USE_JDBC_SERVICE) {
        LOGGER.debug("params array:" + params.toString());
        films = ros.ExecuteQuery(query, params);
      } else {
        films = jdbcPool.ExecuteQuery(query, params);
      }
      return films;
    }
        
    
    public JSONArray FindAvailableStores(int filmId) throws SQLException, JSONException {
	    	try {
		    	String storesQuery = "select distinct store_id from inventory "
		    			+ " where film_id = ? and "
		    			+ " inventory_id not in (select inventory_id from rental where return_date is null)";
		    	JSONArray params = new JSONArray();
		    	RestOverSql.AddIntegerParam(params, filmId);
		    	if (USE_JDBC_SERVICE) {
		    	  if (ros == null) {
		    	    LOGGER.debug("Creating ROS since it is null");
		    	    ros = new RestOverSql();
		    	  }
		    	  return ros.ExecuteQuery(storesQuery, params);
		    	}
		    	// else
		    	return jdbcPool.ExecuteQuery(storesQuery, params);
	    	} catch (Exception e) {
	    		LOGGER.error("FindAvailableStores failed on filmId=" + filmId + "; " + e.toString());
	    	}
	    	return null;
    }
    
    
    public JSONArray FindDues(int userId) throws SQLException, JSONException {
	    	String duesQuery = "select * from rental where return_date is null and customer_id = ?";
	    	JSONArray params = new JSONArray();
	    	RestOverSql.AddIntegerParam(params, userId);
	    	if (USE_JDBC_SERVICE) {
	    	  return ros.ExecuteQuery(duesQuery, params);
	    	}
	    	return jdbcPool.ExecuteQuery(duesQuery, params);
    }    
    
    
    public JSONObject RentMovie(int film_id, int store_id, int duration, int customer_id, int staff_id) throws SQLException, JSONException {
      // Update rental table 
    		int inventoryId = GetInventoryId(film_id, store_id);
    		JSONObject result = new JSONObject();
    		result.put("inventory_id", inventoryId);
    		if (inventoryId < 0) {
    		  return result;
      }
    		
      double rentalRate = GetRentalRate(film_id);
    		JSONObject rentResult = UpdateRental(inventoryId, customer_id, staff_id);
    		int numUpdates = rentResult.getInt("num_updates");
    		result.put("num_updates", numUpdates);
    		if ( numUpdates < 0) {  
    		  return result;
      }
    		result.put("rent", rentalRate*duration);
    		LOGGER.debug("rent movie result returning: " + result.toString());
    		return result;
    }
    
   
    public JSONObject ReturnMovie(int inventoryId, int customerId, int staffId, double rent) {
	    	return ReturnRental(inventoryId, customerId, staffId, rent);
    }
    
    
//    public boolean IsBookBased(String title) {
//    	return ExistsFilm(title);
//    }

    
    // Sales and store performance analysis
    public JSONArray GetSalesByStore(String store_name) throws SQLException, JSONException {
        String query = "select * from sales_by_store where store = '" + store_name + "'";
        	LOGGER.debug(query);
        JSONArray rs = jdbcPool.ExecuteQuery(query);
        return rs;
    }

        
    private static void LoadDriver() throws ClassNotFoundException {
        // This will load the MySQL driver, each DB has its own driver
        Class.forName("com.mysql.jdbc.Driver");
    }
    
  
    public JSONArray RentFilmsBulk(JSONArray filmArray, int storeId, int duration, int customerId, int staffId) throws JSONException, SQLException {
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
	    	  JSONObject obj = this.RentMovie(entry.getValue(), storeId, duration, customerId, staffId);
	    		rentals.put(obj);
	    	}
	    	LOGGER.info(rentals.toString());
	    	return rentals;
    }
    
    
    private int GetInventoryId(int film_id, int store_id) {
	    	int inventory_id = -1;
	    	try {
	    		JSONArray rs = null;
	    		if (USE_PREPARED_STMTS) {
		    		String inventoryQuery = "select inventory_id from inventory "
			    			+ " where film_id = ? and store_id = ? and "
			    			+ " inventory_id not in (select inventory_id from rental where return_date is null) limit 1";
			    	JSONArray params = new JSONArray();
			    	RestOverSql.AddIntegerParam(params, film_id);
			    	RestOverSql.AddIntegerParam(params, store_id);
			    	if (USE_JDBC_SERVICE) {
		        rs = ros.ExecuteQuery(inventoryQuery, params);
			    	} else {
			    	  rs = jdbcPool.ExecuteQuery(inventoryQuery, params);
			    	}
	    		} else {
	    			String inventoryQuery = "select inventory_id from inventory "
			    			+ " where film_id = " + film_id + " and store_id = " + store_id 
			    			+ " and inventory_id not in (select inventory_id from rental where return_date is null) limit 1";
	    			// TODO: jdbc service doesnt support non-prepared statements yet
	    			rs = jdbcPool.ExecuteQuery(inventoryQuery);
	    		}
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

    
    private double GetRentalRate(int film_id) throws SQLException, JSONException {
      String query = "select rental_rate from film where film_id = ?";
      JSONArray params = new JSONArray();
      RestOverSql.AddIntegerParam(params, film_id);
      JSONArray rs = null;
      if (USE_JDBC_SERVICE) {
        rs = ros.ExecuteQuery(query, params);
      } else {
        rs = jdbcPool.ExecuteQuery(query, params);
      }
      if (rs.length() < 1) {
        return -1;
    		}
      return rs.getJSONObject(0).getDouble("rental_rate");
    }
     
    
    private JSONObject UpdateRental(int inventory_id, int customer_id, int staff_id) throws SQLException, JSONException {
        String dateString = format.format(new Date());
        if (USE_PREPARED_STMTS) {
          	String rentalUpdateQuery = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                      + " VALUES (?, ?, ?, ?)";
          	LOGGER.info(rentalUpdateQuery);
          	JSONArray params = new JSONArray();
          	RestOverSql.	AddIntegerParam(params, inventory_id);
          	RestOverSql.AddIntegerParam(params, customer_id);
          	RestOverSql.AddIntegerParam(params, staff_id);
          	RestOverSql.AddStringParam(params, dateString);
          	if (USE_JDBC_SERVICE) {
          	  return ros.ExecuteUpdate(rentalUpdateQuery, params);
          	} 
          	//else {
          	return jdbcPool.ExecuteUpdate(rentalUpdateQuery, params);
        } 
        
        // not USE_PREPARED_STMTS and not USE_JDBC_SERVICE
        String rentalUpdateQuery = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                + " VALUES (" + inventory_id + ", " + customer_id + ", " + staff_id + ", '" + dateString + "')";
        return jdbcPool.ExecuteUpdate(rentalUpdateQuery);
    }
    
    
    private int GetRentalIdForReturn(int inventoryId, int customerId, int staffId) {
      try {
        String rentalIdForReturnQuery = "SELECT rental_id from rental WHERE inventory_id = ? and customer_id = ? and staff_id = ? and return_date is null";
        JSONArray params = new JSONArray();
        RestOverSql.AddIntegerParam(params, inventoryId);
        RestOverSql.AddIntegerParam(params, customerId);
        RestOverSql.AddIntegerParam(params, staffId);
        JSONArray rs = null;
        if (USE_JDBC_SERVICE) {
          rs = ros.ExecuteQuery(rentalIdForReturnQuery, params);
        } else {
          rs = jdbcPool.ExecuteQuery(rentalIdForReturnQuery, params);
        }
        return rs.getJSONObject(0).getInt("rental_id");
      } catch (Exception e) {
        LOGGER.info("Couldn't find rental_id for [" + inventoryId + ", " + customerId + ", " + staffId + "]");
        return -1;
      }
    }
    
    
    private JSONObject ReturnRental(int inventoryId, int customerId, int staffId, double amount) {
      JSONObject result = new JSONObject();
      String dateString = format.format(new Date());
      int rentalId = GetRentalIdForReturn(inventoryId, customerId, staffId);
      result.put("rental_id", rentalId);
      if (rentalId == -1) {
        return result;
      }

      String rentalReturnQuery = "UPDATE rental SET return_date = ? WHERE rental_id = ?";
      JSONArray params1 = new JSONArray();
      RestOverSql.AddStringParam(params1, dateString);
      RestOverSql.AddIntegerParam(params1, rentalId);
      JSONObject returnUpdate = null;
      if (USE_JDBC_SERVICE) {
        returnUpdate = ros.ExecuteUpdate(rentalReturnQuery, params1);
      } else {
        returnUpdate = jdbcPool.ExecuteUpdate(rentalReturnQuery, params1);
      }
      int returnUpdates = returnUpdate.getInt("num_updates");
      result.put("return_updates", returnUpdates);
      if (returnUpdates == -1) {
        return result;  // failure
      }
      
      String paymentQuery = "INSERT INTO payment (customer_id, staff_id, rental_id, amount, payment_date) VALUES (?, ?, ?, ?, ?)";
      JSONArray params2 = new JSONArray();
      RestOverSql.AddIntegerParam(params2, customerId);
      RestOverSql.AddIntegerParam(params2, staffId);
      RestOverSql.AddIntegerParam(params2, rentalId);
      RestOverSql.AddDoubleParam(params2, amount);
      RestOverSql.AddStringParam(params2, dateString);
      JSONObject paymentUpdate = null;
      if (USE_JDBC_SERVICE) {
        paymentUpdate = ros.ExecuteUpdate(paymentQuery, params2);
      } else {
        paymentUpdate = jdbcPool.ExecuteUpdate(paymentQuery, params2);
      }
      result.put("payment_updates", paymentUpdate.getInt("num_updates"));
      return result;
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
