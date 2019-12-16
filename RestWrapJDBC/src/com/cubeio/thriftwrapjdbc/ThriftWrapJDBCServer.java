package com.cubeio.thriftwrapjdbc;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadPoolServer.Args;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import com.cubeio.thriftwrapjdbc.ThriftWrapJDBC.Processor;

public class ThriftWrapJDBCServer {

	private TServer server;

	public void start() throws TTransportException {
		TServerTransport serverTransport = new  TServerSocket(9090);

		System.setProperty("io.md.customer", "ravivj");
		System.setProperty("io.md.instance", "test");
		System.setProperty("io.md.app", "movie-info-thrift");
		System.setProperty("io.md.service", "thriftWrapJdbc");
		System.setProperty("io.md.intent", "record");
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
