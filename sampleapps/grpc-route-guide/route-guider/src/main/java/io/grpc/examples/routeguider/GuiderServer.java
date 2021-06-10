package io.grpc.examples.routeguider;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.FeatureWithNote;
import io.grpc.examples.routeguide.GuiderGrpc.GuiderImplBase;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.Rectangle;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.examples.routeguide.RouteNoteWithNote;
import io.grpc.examples.routeguide.RouteSummary;
import io.grpc.examples.routeguide.RouteSummaryWithNote;
import io.grpc.examples.routeguider.RouteGuideUtil;
import io.grpc.stub.StreamObserver;

public class GuiderServer {

	private static final Logger logger = Logger.getLogger(GuiderServer.class.getName());

	private final int port;
	private final Server server;

	private static final ConcurrentMap<Point, List<RouteNote>> routeNotes =
		new ConcurrentHashMap<Point, List<RouteNote>>();

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

		@Override
		public void listFeaturesWithNote(Rectangle request,
			StreamObserver<FeatureWithNote> responseObserver) {
			int left = min(request.getLo().getLongitude(), request.getHi().getLongitude());
			int right = max(request.getLo().getLongitude(), request.getHi().getLongitude());
			int top = max(request.getLo().getLatitude(), request.getHi().getLatitude());
			int bottom = min(request.getLo().getLatitude(), request.getHi().getLatitude());

			for (Feature feature : features) {
				if (!RouteGuideUtil.exists(feature)) {
					continue;
				}

				int lat = feature.getLocation().getLatitude();
				int lon = feature.getLocation().getLongitude();
				if (lon >= left && lon <= right && lat >= bottom && lat <= top) {
					String randomNote =  UUID.randomUUID().toString();
					FeatureWithNote featureWithNote = FeatureWithNote.newBuilder().setFeature(feature).setRandomNote(randomNote).build();
					responseObserver.onNext(featureWithNote);
				}
			}
			responseObserver.onCompleted();
		}

		@Override
		public StreamObserver<Point> recordRouteWithNote(
			StreamObserver<RouteSummaryWithNote> responseObserver) {
			return new StreamObserver<Point>() {
				int pointCount;
				int featureCount;
				int distance;
				Point previous;
				final long startTime = System.nanoTime();

				@Override
				public void onNext(Point point) {
					pointCount++;
					if (RouteGuideUtil.exists(checkFeature(point))) {
						featureCount++;
					}
					// For each point after the first, add the incremental distance from the previous point to
					// the total distance value.
					if (previous != null) {
						distance += calcDistance(previous, point);
					}
					previous = point;
					System.out.println("Received point: " + point.toString());
				}

				@Override
				public void onError(Throwable t) {
					logger.log(Level.WARNING, "recordRoute cancelled");
				}

				@Override
				public void onCompleted() {
					String randomNote =  UUID.randomUUID().toString();
					long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
					RouteSummaryWithNote routeSummaryWithNote = RouteSummaryWithNote.newBuilder().setRouteSummary(
						RouteSummary.newBuilder().setPointCount(pointCount)
							.setFeatureCount(featureCount).setDistance(distance)
							.setElapsedTime((int) seconds).build()
					)
						.setRandomNote(randomNote)
						.build();
					System.out.println("Returning routeSummaryWithNote: " + routeSummaryWithNote.toString());
					responseObserver.onNext(routeSummaryWithNote);
					responseObserver.onCompleted();
				}
			};

		}

		@Override
		public StreamObserver<RouteNote> routeChatWithNote(
			StreamObserver<RouteNoteWithNote> responseObserver) {
			return new StreamObserver<RouteNote>() {
				@Override
				public void onNext(RouteNote note) {
					System.out.println("Received note: " + note.toString());

					List<RouteNote> notes = getOrCreateNotes(note.getLocation());

					String randomNote =  UUID.randomUUID().toString();

					// Respond with all previous notes at this location.
					for (RouteNote prevNote : notes.toArray(new RouteNote[0])) {
						RouteNoteWithNote routeNoteWithNote = RouteNoteWithNote.newBuilder().setRouteNote(prevNote)
							.setRandomNote(randomNote).build();
						System.out.println("Returning routeNoteWithNote: " + routeNoteWithNote.toString());
						responseObserver.onNext(routeNoteWithNote);
					}

					// Now add the new note to the list
					notes.add(note);
				}

				@Override
				public void onError(Throwable t) {
					logger.log(Level.WARNING, "routeChat cancelled");
				}

				@Override
				public void onCompleted() {
					System.out.println("Completed Stream routeNoteWithNote");
					responseObserver.onCompleted();
				}
			};
		}

		/**
		 * Gets the feature at the given point.
		 *
		 * @param location the location to check.
		 * @return The feature object at the point. Note that an empty name indicates no feature.
		 */
		private Feature checkFeature(Point location) {
			for (Feature feature : features) {
				if (feature.getLocation().getLatitude() == location.getLatitude()
					&& feature.getLocation().getLongitude() == location.getLongitude()) {
					return feature;
				}
			}

			// No feature was found, return an unnamed feature.
			return Feature.newBuilder().setName("").setLocation(location).build();
		}

		/**
		 * Calculate the distance between two points using the "haversine" formula. The formula is
		 * based on http://mathforum.org/library/drmath/view/51879.html.
		 *
		 * @param start The starting point
		 * @param end   The end point
		 * @return The distance between the points in meters
		 */
		private static int calcDistance(Point start, Point end) {
			int r = 6371000; // earth radius in meters
			double lat1 = toRadians(RouteGuideUtil.getLatitude(start));
			double lat2 = toRadians(RouteGuideUtil.getLatitude(end));
			double lon1 = toRadians(RouteGuideUtil.getLongitude(start));
			double lon2 = toRadians(RouteGuideUtil.getLongitude(end));
			double deltaLat = lat2 - lat1;
			double deltaLon = lon2 - lon1;

			double a = sin(deltaLat / 2) * sin(deltaLat / 2)
				+ cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2);
			double c = 2 * atan2(sqrt(a), sqrt(1 - a));

			return (int) (r * c);
		}

		/**
		 * Get the notes list for the given location. If missing, create it.
		 */
		private List<RouteNote> getOrCreateNotes(Point location) {
			List<RouteNote> notes = Collections.synchronizedList(new ArrayList<RouteNote>());
			List<RouteNote> prevNotes = routeNotes.putIfAbsent(location, notes);
			return prevNotes != null ? prevNotes : notes;
		}
	}
}
