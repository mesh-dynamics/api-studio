package com.cubeiosample.webservices.rest.jersey;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private Connection connect = null;
    private Statement statement = null;

    public DBConnection(String username, String pwd, String dbname, String uri) {
        try {
            // Setup the connection with the DB
            String conn_string = uri + "/" + dbname + "?user=" + username + "&password=" + pwd;
            // TODO: figure out how to set certificates
            conn_string += "&verifyServerCertificate=false&useSSL=false&requireSSL=false";
            System.out.println(conn_string);
            connect = DriverManager.getConnection(conn_string);
        } catch (Exception e) {
            // TODO: log e
            System.out.println(e.toString());
        }
    }

    public ResultSet ExecuteQuery(String query) throws SQLException {
        statement = connect.createStatement();
        return statement.executeQuery(query);
    }

    public int UpdateQuery(String query) throws SQLException {
        statement = connect.createStatement();
        int num_updates = statement.executeUpdate(query);
        return num_updates;
    }

    public void close() throws SQLException {
        connect.close();
    }
}