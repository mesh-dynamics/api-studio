package com.cubeio.thriftwrapjdbc;

import java.util.Map;
import java.util.Properties;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

public class ThriftWrapJDBCServer {

    private TServer server;

    public void start() throws TTransportException {
        TServerTransport serverTransport = new TServerSocket(9090);
        /*THttpClient*/
        /*Properties properties = new Properties();
        properties.putAll(Map.of("cubeCustomerId", "ravivj" , "cubeInstanceId" ,
            "test" , "cubeAppName", "thriftWrapJdbc" , "cubeServiceName"
            , "thriftWrapJdbc" , "intent" , "record"));*/

        System.setProperty("cubeCustomerId", "ravivj");
        System.setProperty("cubeInstanceId", "test");
        System.setProperty("cubeAppName", "thriftWrapJdbc");
        System.setProperty("cubeServiceName", "thriftWrapJdbc");
        System.setProperty("intent", "record");
        server = new TSimpleServer(new TServer.Args(serverTransport)
            .processor(new ThriftWrapJDBC.Processor<>(new ThriftWrapJDBCHandler())));

        System.out.print("Starting the server... ");

        server.serve();

        System.out.println("done.");
    }

    public void stop() {
        if (server != null && server.isServing()) {
            System.out.print("Stopping the server... ");

            server.stop();

            System.out.println("done.");
        }
    }

    public static void main(String[] args) {
        ThriftWrapJDBCServer server = new ThriftWrapJDBCServer();
        try {
            server.start();
        } catch (TTransportException e) {
            System.out.println("Server has stopped due to exception :: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
