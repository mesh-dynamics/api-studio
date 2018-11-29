package com.cubeiosample.webservices.rest.jersey;

import java.sql.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.json.*;

public class MovieRentals {

    private DBConnection myConnection;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MovieRentals() throws ClassNotFoundException {
    	// TODO: initialize a set of connections in pool and each of the requests should pick from that pool.
    	LoadDriver();
    	myConnection = new DBConnection("vganti", "password", "sakila", "jdbc:mysql://127.0.0.1:3306");
    }
    
    private static void LoadDriver() throws ClassNotFoundException {
        // This will load the MySQL driver, each DB has its own driver
        Class.forName("com.mysql.jdbc.Driver");
    }


    public void Connect(String username, String pwd, String dbname, String server_uri) {
        myConnection = new DBConnection(username, pwd, dbname, server_uri);  // also do this with a kerberos connection
    }

    public JSONArray GetSalesByStore(String store_id) throws SQLException, JSONException {
        String query = "select * from sales_by_store where store = '" + store_id + "'";
        ResultSet rs = myConnection.ExecuteQuery(query);
        return ConvertResultSetToJson(rs);
    }


    // returns rental_id if successful; otherwise -1 if film_id is not available in the store_id
    public double RentMovie(int film_id, int store_id, int duration, int customer_id, int staff_id) throws SQLException {
        // Update rental table and then payment table
        // Note: last_update columns in both tables are auto-update columns
        int inventory_id = GetInventoryId(film_id, store_id);
        if (inventory_id < 0) {
            return -1;
        }
        double rental_rate = GetRentalRate(film_id);
        int num_updates = UpdateRental(inventory_id, customer_id, staff_id);
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
        ResultSet rs = myConnection.ExecuteQuery(query);
        int inventory_id = -1;
        while (rs.next()) {
            inventory_id = rs.getInt(1);
        }
        return inventory_id;
    }

    private double GetRentalRate(int film_id) throws SQLException {
        String query = "select rental_rate from film where film_id = " + film_id;
        double rr = -1.0;
        ResultSet rs = myConnection.ExecuteQuery(query);
        while (rs.next()) {
            rr = rs.getDouble(1);
        }
        return rr;
    }

    private int UpdateRental(int inventory_id, int customer_id, int staff_id) throws SQLException {
        String dateString = format.format(new Date());
        String update_query = "INSERT INTO rental (inventory_id, customer_id, staff_id, rental_date) "
                + " VALUES (" + inventory_id + ", " + customer_id + ", " + staff_id + ", '" + dateString + "')";
        System.out.println(update_query);
        return myConnection.UpdateQuery(update_query);
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
            return null;
        }
    }
}
