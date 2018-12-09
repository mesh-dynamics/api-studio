package com.cubeiosample.webservices.rest.jersey;

import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

import org.json.*;

public class MovieRentals {

    private ConnectionPool jdbcPool = null;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private boolean USE_PREPARED_STMTS = true;    
    // TODO: couldn't figure out how to configure MovieRentals.class
    final static Logger LOGGER = Logger.getLogger(MovieRentals.class);
    
    public MovieRentals() throws ClassNotFoundException {
    	LoadDriver();
    	
    	// TODO: make a separate database query service.
    	jdbcPool = new ConnectionPool();
        try {   
        	// TODO: move this to the query service
            jdbcPool.setUpPool("jdbc:mysql://127.0.0.1:3306/sakila", "cube", "cubeio");
            LOGGER.info(jdbcPool.getPoolStatus());
        } catch (Exception e) {
        	LOGGER.error("connection pool creation failed; " + e.toString());
        }
    }
    
    
    public JSONArray ListMovies(String filmName, String keyword) {
    	// TODO: add actor also in the parameter options.
    	try {
	    	if (filmName != null && !filmName.isEmpty()) {
	    		// query with filmname
	    		String query = "select film_id, title from film where title = ?";
	    		JSONArray params = new JSONArray();
	    		AddStringParam(params, filmName);
	    		JSONArray films = jdbcPool.ExecuteQuery(query, params);
	    		if (films != null && films.length() > 0) {
	    			return films;
	    		}
	    	}
	    	if (keyword != null && !keyword.isEmpty()) {
	    		String query = "select id, title from film where title like ?";
	    		JSONArray params = new JSONArray();
	    		AddStringParam(params, filmName);
	    		JSONArray films = jdbcPool.ExecuteQuery(query, params);
	    		if (films != null && films.length() > 0) {
	    			return films;
	    		}
	    	}
    	} catch (Exception e) {
    		LOGGER.error("Couldn't list movies; " + e.toString());
    	}
    	return null;
    }
    
    
    public JSONArray FindAvailableStores(int filmId) throws SQLException, JSONException {
    	try {
	    	String storesQuery = "select distinct store_id from inventory "
	    			+ " where film_id = ? and "
	    			+ " inventory_id not in (select inventory_id from rental where return_date is null)";
	    	JSONArray params = new JSONArray();
	    	AddIntegerParam(params, filmId);
	    	return jdbcPool.ExecuteQuery(storesQuery, params);
    	} catch (Exception e) {
    		LOGGER.error(e.toString());
    	}
    	return null;
    }
    
    
    public JSONArray FindDues(int userId) throws SQLException, JSONException {
    	String duesQuery = "select * from rental where return_date is null and customer_id = ?";
    	JSONArray params = new JSONArray();
    	AddIntegerParam(params, userId);
    	return jdbcPool.ExecuteQuery(duesQuery, params);
    }
    
    
    
    
    // returns rental amount if successful; otherwise -1 if film_id is not available in the store_id
    public double RentMovie(int film_id, int store_id, int duration, int customer_id, int staff_id) throws SQLException, JSONException {
        // Update rental table and then payment table
        // Note: last_update columns in both tables are auto-update columns
    	int inventory_id = -1;
    	inventory_id = GetInventoryId(film_id, store_id);
    	
        if (inventory_id < 0) {
            return -1;
        }
        
        double rental_rate = GetRentalRate(film_id);
        int num_updates = 0;
        num_updates = UpdateRental(inventory_id, customer_id, staff_id);
        
        if (num_updates < 0) {
            return 0.0;
        }
        return rental_rate * duration;
    }
    
   
    public int ReturnMovie(int filmId, int storeId, int customerId, double rent) {
    	// TODO
    	return 1;
    }
    
    
//    public boolean IsBookBased(String title) {
//    	return ExistsFilm(title);
//    }

    
    // Sales and store performance analysis
    public JSONArray GetSalesByStore(String store_name) throws SQLException, JSONException {
        String query = "select * from sales_by_store where store = '" + store_name + "'";
        if (LOGGER.isDebugEnabled()) {
        	LOGGER.debug(query);
        }
        JSONArray rs = jdbcPool.ExecuteQuery(query);
        return rs;
    }

    
    private void AddStringParam(JSONArray params, String value) throws JSONException {
    	JSONObject param = new JSONObject();
    	param.put("index", params.length() + 1);
    	param.put("type", "string");
    	param.put("value", value);
		params.put(param);
    }
    
    private void AddIntegerParam(JSONArray params, Integer value) throws JSONException {
    	JSONObject param = new JSONObject();
    	param.put("index", params.length() + 1);
    	param.put("type", "integer");
    	param.put("value", value);
		params.put(param);
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
    		double rent = this.RentMovie(entry.getValue(), storeId, duration, customerId, staffId);
    		JSONObject obj = new JSONObject();
    		obj.put(entry.getKey(), rent);
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
		    	AddIntegerParam(params, film_id);
		    	AddIntegerParam(params, store_id);
		    	rs = jdbcPool.ExecuteQuery(inventoryQuery, params);
		    	
    		} else {
    			String inventoryQuery = "select inventory_id from inventory "
		    			+ " where film_id = " + film_id + " and store_id = " + store_id 
		    			+ " and inventory_id not in (select inventory_id from rental where return_date is null) limit 1";
    			rs = jdbcPool.ExecuteQuery(inventoryQuery);
    		}
    		if (rs != null && rs.length() < 1) {
	    		return -1;
	    	}
	    	return rs.getJSONObject(0).getInt("inventory_id");
    	} catch (Exception sqlException) {
    		LOGGER.error("Couldn't prepare rental update stmt: " + sqlException.toString());
    	}
    	return inventory_id;
    }

    
    private double GetRentalRate(int film_id) throws SQLException, JSONException {
        String query = "select rental_rate from film where film_id = " + film_id;
        JSONArray rs = jdbcPool.ExecuteQuery(query);
        if (rs.length() < 1) {
    		return -1;
    	}
        return rs.getJSONObject(0).getDouble("rental_rate");
    }
     
    
    private int UpdateRental(int inventory_id, int customer_id, int staff_id) throws SQLException, JSONException {
        String dateString = format.format(new Date());
        if (USE_PREPARED_STMTS) {
        	String rentalUpdateQuery = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                    + " VALUES (?, ?, ?, ?)";
        	LOGGER.info(rentalUpdateQuery);
        	JSONArray params = new JSONArray();
        	AddIntegerParam(params, inventory_id);
        	AddIntegerParam(params, customer_id);
        	AddIntegerParam(params, staff_id);
        	AddStringParam(params, dateString);
        	return jdbcPool.ExecuteUpdate(rentalUpdateQuery, params);
        } 
        String rentalUpdateQuery = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                + " VALUES (" + inventory_id + ", " + customer_id + ", " + staff_id + ", '" + dateString + "')";
        return jdbcPool.ExecuteUpdate(rentalUpdateQuery);
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
