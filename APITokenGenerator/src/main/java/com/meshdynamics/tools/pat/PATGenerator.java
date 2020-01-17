package com.meshdynamics.tools.pat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class PATGenerator {

    private long validityInSeconds = 60 * 60 * 24 * 365 * 10; // 10 years
    private String secretKey = Base64.getEncoder().encodeToString("prod-secret".getBytes());
    private String dbURL = "jdbc:postgresql://cubeuibackend.cxg1ykcvrpg9.us-east-2.rds.amazonaws.com:5432/cubeproddb?schema=qui";
    private String dbUser = "springboot";
    private String dbPassword = "2XPZxVqH9Fm%o$#";

    private Connection dbConnection;

    public PATGenerator (Properties props) throws Exception {
        dbURL = props.getProperty("url");
        dbUser = props.getProperty("username");
        dbPassword = props.getProperty("password");
        secretKey = props.getProperty("secret");
        dbConnection = initializeDBConnection();
    }

    private Connection initializeDBConnection () throws Exception {
        try {
            Connection connection = DriverManager
                .getConnection(dbURL, dbUser, dbPassword);
            Class.forName("org.postgresql.Driver");
            return connection;
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC driver not found.");
            e.printStackTrace();
            throw e;
        } catch (SQLException e) {
            System.out.println("Connection failure.");
            e.printStackTrace();
            throw e;
        }
    }

    private int checkAndInsertAgentUser(int customerId) throws Exception {
        try {
            String sql ="SELECT * FROM public.users WHERE name = ? AND customer_id = ?";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setString(1, "MeshDAgentUser");
            preparedStatement.setInt(2, customerId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next() == true) {
                System.out.println("Agent user is already there");
                return resultSet.getInt("id");
            } else {
                //Agent user is not there, insert it into the database
                String generatedColumns[] = { "id" };
                String insertPStmt = "INSERT INTO public.users ( activated,  name, username, customer_id)" + "VALUES (?, ?, ? , ?)";
                preparedStatement = dbConnection.prepareStatement(insertPStmt, generatedColumns);
                preparedStatement.setBoolean(1, false);
                preparedStatement.setString(2, "MeshDAgentUser");
                preparedStatement.setString(3, "MeshDAgentUser");
                preparedStatement.setInt(4, customerId);
                int row = preparedStatement.executeUpdate();
                if (row == 1 ) {
                    System.out.println("Agent user inserted. Rows count :" + row);
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    if (rs.next()) {
                        return rs.getInt(1);
                    } else {
                        throw new Exception ("Insert agent user failed : Rows count" + row);
                    }
                } else {
                    throw new Exception ("Insert agent user failed : Rows count" + row);
                }
            }

        } catch (SQLException sqe) {
            throw sqe;
        }
    }

    private int insertOrUpdatePAT(int userId, boolean updateToken) throws Exception {
        try {
            String sql = "SELECT * FROM public.api_access_tokens WHERE user_id = ?";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next() == true) {
                System.out.println("There is an existing token : " + resultSet.getString("token"));
                if (updateToken) {
                    String updateSql = "UPDATE public.api_access_tokens SET token = ? WHERE user_id = ?";
                    String token = generateToken();
                    preparedStatement = dbConnection.prepareStatement(token);
                    preparedStatement.setString(1, generateToken());
                    preparedStatement.setInt(2, userId);
                    int row = preparedStatement.executeUpdate();
                    if (row == 1 ) {
                        System.out.println("Token updated in the DB for Agent user");
                        System.out.println("Token : " + token);
                    } else {
                        throw new Exception ("Some has gone wrong with Token update for agent user. Rows count:" + row);
                    }

                } else {
                    System.out.println("Update Token is false, so doing nothing");
                }
            } else {
                String generatedColumns[] = { "ID" };
                String insertPStmt = "INSERT INTO public.api_access_tokens ( token , user_id)" + "VALUES (?, ?)";
                String token = generateToken();
                preparedStatement = dbConnection.prepareStatement(insertPStmt);
                preparedStatement.setString(1, token);
                preparedStatement.setInt(2, userId);
                int row = preparedStatement.executeUpdate();
                if (row == 1 ) {
                    System.out.println("Token inserted for agent user. Rows count :" + row);
                    System.out.println("Token : " + token);
                } else {
                    throw new Exception ("Some has gone wrong with Token insertion for agent user. Rows count:" + row);
                }
            }
        } catch (SQLException sqe) {
            throw sqe;
        }
        return 0;
    }

    public String generateToken() {

        Set<String> roles = new HashSet<String>();
        roles.add("ROLE_USER");
        Claims claims = Jwts.claims().setSubject("MeshDAgentUser");
        claims.put("roles", roles);
        claims.put("type", "pat");
        claims.put("randomstr", getAlphaNumericString(10));

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInSeconds * 1000);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    static String getAlphaNumericString(int n)
    {

        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789"
            + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                = (int)(AlphaNumericString.length()
                * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                .charAt(index));
        }

        return sb.toString();
    }

    public static void main(String[] args) throws  Exception {
        String configPath;
        String customerIdStr;

        if (args.length != 2 ) {
            System.out.println("Enter the config file path");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            configPath = reader.readLine();

            System.out.println("Enter the Customer Id for whom API token to be generated");
            reader = new BufferedReader(new InputStreamReader(System.in));

            customerIdStr = reader.readLine();
        } else {
            configPath = args[0];
            customerIdStr = args[1];
        }

        Properties prop = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(configPath);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.exit(2);
        }
        try {
            prop.load(is);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(3);
        }
        int customerId=0;

        try {
            customerId = Integer.parseInt(customerIdStr);
        } catch (NumberFormatException e) {
            System.out.println("Customer Id should be a numeric value");
            System.exit(1);
        }

        PATGenerator patGenerator = new PATGenerator(prop);
        int userId = patGenerator.checkAndInsertAgentUser(customerId);
        patGenerator.insertOrUpdatePAT(userId, false);
    }
}
