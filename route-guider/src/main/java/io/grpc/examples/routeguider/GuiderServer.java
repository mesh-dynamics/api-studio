package io.grpc.examples.routeguider;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.FeatureWithNote;
import io.grpc.examples.routeguide.GuiderGrpc.GuiderImplBase;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguider.RouteGuideUtil;
import io.grpc.stub.StreamObserver;

public class GuiderServer {

	private static final Logger logger = Logger.getLogger(GuiderServer.class.getName());

	private final int port;
	private final Server server;

	public GuiderServer(int port) throws IOException {
		this(port, RouteGuideUtil.getDefaultFeaturesFile());
	}

	/**
	 * Create a RouteGuide server listening on {@code port} using {@code featureFile} database.
	 */
	public GuiderServer(int port, URL featureFile) throws IOException {
		this(ServerBuilder.forPort(port), port, RouteGuideUtil.parseFeatures(featureFile));
	}

	/**
	 * Create a RouteGuide server using serverBuilder as a base and features as data.
	 */
	public GuiderServer(ServerBuilder<?> serverBuilder, int port,
		Collection<Feature> features) {
		this.port = port;
		server = serverBuilder.addService(
			ServerInterceptors.intercept(new GuiderService(features)))
			.build();
	}

	/**
	 * Start serving requests.
	 */
	public void start() throws IOException {
		server.start();
		logger.info("Server started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				try {
					GuiderServer.this.stop();
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
				System.err.println("*** server shut down");
			}
		});
	}

	/**
	 * Stop serving requests and shutdown resources.
	 */
	public void stop() throws InterruptedException {
		if (server != null) {
			server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon threads.
	 */
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	/**
	 * Main method.  This comment makes the linter happy.
	 */
	public static void main(String[] args) throws Exception {
		GuiderServer server = new GuiderServer(8981);
		server.start();
		server.blockUntilShutdown();
	}

	/**
	 * Our implementation of RouteGuide service.
	 *
	 * <p>See route_guide.proto for details of the methods.
	 */
	private static class GuiderService extends GuiderImplBase {

		private final Collection<Feature> features;

		GuiderService(Collection<Feature> features) {
			this.features = features;
		}

		/**
		 * Gets the {@link Feature} at the requested {@link Point}. If no feature at that location
		 * exists, an unnamed feature is returned at the provided location.
		 *
		 * @param request          the requested location for the feature.
		 * @param responseObserver the observer that will receive the feature at the requested
		 *                         point.
		 */
		@Override
		public void getFeatureWithNote(Feature request, StreamObserver<FeatureWithNote> responseObserver) {
			System.out.println("Request received to getFeatureGuider for point\n " + request.toString());
			String randomNote = "I'm a random happy string added to create differences."
				+ " I'm sorry that's my job to create differences."
				+ " Here is it: " + UUID.randomUUID().toString();

			FeatureWithNote featureWithNote = FeatureWithNote.newBuilder().setFeature(request).setRandomNote(randomNote).build();
			responseObserver.onNext(featureWithNote);
			responseObserver.onCompleted();
		}
	}
}
