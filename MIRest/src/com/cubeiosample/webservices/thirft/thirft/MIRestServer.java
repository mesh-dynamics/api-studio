package com.cubeiosample.webservices.thirft.thirft;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

public class MIRestServer {

    private TServer server;

    public void start() throws TTransportException {
        TServerTransport serverTransport = new TServerSocket(9090);
        /*THttpClient*/
        server = new TSimpleServer(new TServer.Args(serverTransport)
                .processor(new MIRest.Processor<>(new MIThriftService())));

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
        MIRestServer server = new MIRestServer();
        try {
            server.start();
        } catch (TTransportException e) {
            System.out.println("Server has stopped due to exception :: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
