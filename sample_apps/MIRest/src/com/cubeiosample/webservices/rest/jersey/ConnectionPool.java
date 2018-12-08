package com.cubeiosample.webservices.rest.jersey;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;
 
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import com.mysql.jdbc.Statement;
 
public class ConnectionPool {
 
    // JDBC Driver Name & Database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    final static Logger LOGGER = Logger.getLogger(ConnectionPool.class);
    
    private static GenericObjectPool gPool = null;
    
    private DataSource connPool = null;

    @SuppressWarnings("unused")
    public void setUpPool(String uri, String user, String pwd) throws Exception {
        Class.forName(JDBC_DRIVER);
        // Creates an Instance of GenericObjectPool That Holds Our Pool of Connections Object!
        gPool = new GenericObjectPool();
        gPool.setMaxActive(5);

        // Creates a ConnectionFactory Object Which Will Be Use by the Pool to Create the Connection Object!
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pwd);
        props.setProperty("verifyServerCertificate", "false");
        props.setProperty("useSSL", "false");
        props.setProperty("requireSSL", "false");
        ConnectionFactory cf = new DriverManagerConnectionFactory(uri, props);

        // Creates a PoolableConnectionFactory That Will Wraps the Connection Object Created by the ConnectionFactory to Add Object Pooling Functionality!
        PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf, gPool, null, null, false, true);
        connPool = new PoolingDataSource(gPool);
    }
 
    public GenericObjectPool getConnectionPool() {
        return gPool;
    }
 
    // This Method Is Used To Print The Connection Pool Status
    public String getPoolStatus() {
        return "Max.: " + getConnectionPool().getMaxActive() + "; Active: " + getConnectionPool().getNumActive() + "; Idle: " + getConnectionPool().getNumIdle();
    }
    
    
    // TODO: move to a different class after creating a new DataService.
    public PreparedStatement GetPreparedStatement(String query) throws SQLException {
    	return connPool.getConnection().prepareStatement(query);
    }
    
    public JSONArray ExecuteQuery(String query, JSONArray params) {
    	try {
    		PreparedStatement stmt = connPool.getConnection().prepareStatement(query);
    		for (int i = 0; i < params.length(); ++i) {
    			JSONObject obj = params.getJSONObject(i);
    			BindParameter(stmt, obj);
    		}
	    	ResultSet rs = stmt.executeQuery();
	    	return ConvertResultSetToJson(rs);
    	} catch (Exception e) {
    		LOGGER.error("couldn't executy query " + e.toString());
    	}
    	return null;
    }
    
    public JSONArray ExecuteQuery(String query) throws SQLException, JSONException {
    	Statement stmt = connPool.getConnection().createStatement();
    	ResultSet rs = stmt.executeQuery(query);
    	return ConvertResultSetToJson(rs);
    }
    
    
    public int ExecuteUpdate(String query, JSONArray params) {
    	try {
    		System.out.println("update query: " + query);
    		System.out.println(params.toString());
    		PreparedStatement stmt = connPool.getConnection().prepareStatement(query);
    		for (int i = 0; i < params.length(); ++i) {
    			JSONObject obj = params.getJSONObject(i);
    			BindParameter(stmt, obj);
    		}
	    	return stmt.executeUpdate();
    	} catch (Exception e) {
    		LOGGER.error(e.toString());
    	}
    	return -1;
    }
    
    public int ExecuteUpdate(String query) {
    	try {
    		Statement stmt = connPool.getConnection().createStatement();
    		return stmt.executeUpdate(query);
    	} catch (Exception e) {
    		LOGGER.error(e.toString());
    	}
		return -1;
    }
    
    
    private void BindParameter(PreparedStatement stmt, JSONObject param) throws JSONException, SQLException {
    	int index = param.getInt("index");
    	String dataType = param.getString("type").toLowerCase();
    	
    	switch(dataType) {
	    	case "string": 
	    		stmt.setString(index, param.getString("value"));
	    		break;
	    	
	    	case "integer":
	    		stmt.setInt(index, param.getInt("value"));
	    		break;
	    	
	    	case "double": 
	    		stmt.setDouble(index, param.getDouble("value"));
	    		break;
    	}
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
