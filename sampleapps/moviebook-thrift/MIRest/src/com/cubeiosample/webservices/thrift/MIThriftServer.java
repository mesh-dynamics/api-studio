package com.cubeiosample.webservices.thrift;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

public class MIThriftServer {

	private TServer server;

	public void start() throws TTransportException {

		System.setProperty("io.md.customer", "ravivj");
		System.setProperty("io.md.instance", "test");
		System.setProperty("io.md.app", "movie-info-thrift");
		System.setProperty("io.md.service", "miThrift");
		System.setProperty("io.md.intent", "record");

		TServerTransport serverTransport = new TServerSocket(9090);
		/*THttpClient*/
		server = new TSimpleServer(new TServer.Args(serverTransport)
			.processor(new MIThrift.Processor<>(new MIThriftHandler())));

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
		MIThriftServer server = new MIThriftServer();
		try {
			server.start();
		} catch (TTransportException e) {
			System.out.println("Server has stopped due to exception :: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
