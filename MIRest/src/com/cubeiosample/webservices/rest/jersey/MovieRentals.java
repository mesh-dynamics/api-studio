package com.cubeiosample.webservices.rest.jersey;

import java.sql.*;
import java.util.Date;

import javax.sql.DataSource;

import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

import org.json.*;

public class MovieRentals {

    private DataSource connPool = null;
    private PreparedStatement inventory_stmt = null;
    private PreparedStatement rental_update_stmt = null;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private boolean USE_PREPARED_STMTS = true;    
    // TODO: couldn't figure out how to configure MovieRentals.class
    final static Logger LOGGER = Logger.getLogger(MovieRentals.class);
    
    public MovieRentals() throws ClassNotFoundException {
    	// TODO: initialize a set of connections in pool and each of the requests should pick from that pool.
    	LoadDriver();
    	ConnectionPool jdbcPool = new ConnectionPool();
        try {   
            connPool = jdbcPool.setUpPool("jdbc:mysql://127.0.0.1:3306/sakila", "cube", "cubeio");
            LOGGER.info(jdbcPool.getPoolStatus());
    
            // setup prepared statements
            this.PrepareInventoryStatement();
            this.PrepareRentalUpdateStmt();
        } catch (Exception e) {
        	LOGGER.error("connection pool creation failed; " + e.toString());
        }
    }
    
    
    private static void LoadDriver() throws ClassNotFoundException {
        // This will load the MySQL driver, each DB has its own driver
        Class.forName("com.mysql.jdbc.Driver");
    }

    
    public JSONArray GetSalesByStore(String store_name) throws SQLException, JSONException {
        String query = "select * from sales_by_store where store = '" + store_name + "'";
        ResultSet rs = connPool.getConnection().createStatement().executeQuery(query);
        if (LOGGER.isDebugEnabled()) {
        	LOGGER.debug(query);
        }
        return ConvertResultSetToJson(rs);
    }


    // returns rental_id if successful; otherwise -1 if film_id is not available in the store_id
    public double RentMovie(int film_id, int store_id, int duration, int customer_id, int staff_id) throws SQLException {
        // Update rental table and then payment table
        // Note: last_update columns in both tables are auto-update columns
    	int inventory_id = -1;
    	if (USE_PREPARED_STMTS) {
    		inventory_id = GetInventoryIdPrepStmt(film_id, store_id);
    	} else {
    		inventory_id = GetInventoryId(film_id, store_id);
    	}
    	
        if (inventory_id < 0) {
            return -1;
        }
        
        double rental_rate = GetRentalRate(film_id);
        int num_updates = 0;
        if (USE_PREPARED_STMTS) {
        	num_updates = UpdateRentalPrepStmt(inventory_id, customer_id, staff_id);
        } else {
        	num_updates = UpdateRental(inventory_id, customer_id, staff_id);
        }
        if (num_updates < 0) {
            return 0.0;
        }
        return rental_rate * duration;
    }
    

    // TODO: create a hashmap and iterate over it to simulate different auto-created ids
    public void RentMoviesBulk() {}
     
    
    private int GetInventoryId(int film_id, int store_id) throws SQLException {
        // find inventory ids for the film in a given store that haven't been rented out.
        String query = "select inventory_id from inventory where "
                + " film_id = " + film_id
                + " and store_id = " + store_id
                + " and inventory_id not in (select inventory_id from rental where return_date is null) limit 1;";
        ResultSet rs = connPool.getConnection().createStatement().executeQuery(query);
        int inventory_id = -1;
        while (rs.next()) {
            inventory_id = rs.getInt(1);
        }
        return inventory_id;
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

    
    private int GetInventoryIdPrepStmt(int film_id, int store_id) {
    	int inventory_id = -1;
    	try {
	    	inventory_stmt.setInt(1, film_id);
	    	inventory_stmt.setInt(2, store_id);
	    	ResultSet rs = inventory_stmt.executeQuery();
	    	while (rs.next()) {
	    		inventory_id = rs.getInt(1);
	    	}
    	} catch (Exception sqlException) {
    		LOGGER.error("Couldn't prepare rental update stmt: " + sqlException.toString());
    	}
    	return inventory_id;
    }

    
    private double GetRentalRate(int film_id) throws SQLException {
        String query = "select rental_rate from film where film_id = " + film_id;
        double rr = -1.0;
        ResultSet rs = connPool.getConnection().createStatement().executeQuery(query);
        while (rs.next()) {
            rr = rs.getDouble(1);
        }
        return rr;
    }

    
    private int UpdateRental(int inventory_id, int customer_id, int staff_id) throws SQLException {
        String dateString = format.format(new Date());
        String update_query = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                + " VALUES (" + inventory_id + ", " + customer_id + ", " + staff_id + ", '" + dateString + "')";
        if (LOGGER.isDebugEnabled()) {
        	LOGGER.debug(update_query);
        }
        return connPool.getConnection().createStatement().executeUpdate(update_query);
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
}
