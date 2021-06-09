package com.cubeiosample.webservices.thrift;

import java.util.Arrays;
import java.util.Optional;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class MIThriftClient {

	public static void main(String[] args) {
		TTransport transport = new TSocket("localhost", 9090);

		try {
			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			MIThrift.Client client = new MIThrift.Client(protocol);

			/*String[] movies = {"ANACONDA CONFESSIONS", "AUTUMN CROW", "BEVERLY OUTLAW",
				"BOWFINGER GABLES", "CAMPUS REMEMBER", "CHARIOTS CONSPIRACY", "CLUE GRAIL", "CORE SUIT",
				"DANGEROUS UPTOWN", "DIARY PANIC", "DRIFTER COMMANDMENTS", "ELEMENT FREDDY",
				"FACTORY DRAGON", "FLATLINERS KILLER", "GABLES METROPOLIS", "GONE TROUBLE", "HALF OUTFIELD",
				"HELLFIGHTERS SIERRA", "HOUSE DYNAMITE", "INNOCENT USUAL", "JERICHO MULAN", "LADY STAGE",
				"LONELY ELEPHANT", "MAJESTIC FLOATS", "MIDSUMMER GROUNDHOG", "MOSQUITO ARMAGEDDON",
				"NETWORK PEAK", "OSCAR GOLD", "PEACH INNOCENT", "POND SEATTLE", "RAINBOW SHOCK",
				"ROBBERY BRIGHT", "SALUTE APOLLO", "SHAKESPEARE SADDLE", "SLEEPLESS MONSOON",
				"SPIKING ELEMENT", "STRAIGHT HOURS", "TADPOLE PARK", "TORQUE BOUND", "UNBREAKABLE KARATE",
				"VILLAIN DESPERATE", "WEDDING APOLLO", "WORKING MICROCOSMOS"};*/

			String[] movies = {"ANACONDA CONFESSIONS", "AUTUMN CROW", "BEVERLY OUTLAW",
				"BOWFINGER GABLES", "CAMPUS REMEMBER"};

			Arrays.stream(movies).forEach(movie -> {
				try {
					ListMovieResult result = client
						.listMovies(movie, null, null, null);
					Optional.ofNullable(result.movieInfoList).ifPresent(storeInfoList
						-> storeInfoList
						.forEach(x -> System.out.println(x.displayActors.toString())));
				}  catch (Exception e) {
					System.out.println("ERROR OCCURED :: " + e.getMessage());
				}
			});


//
//            //ListStoreResult result = client.listStores(23);
//


			//System.out.println(client.healthCheck());
			transport.close();
		} catch (TException e) {
			e.printStackTrace();
		}


	}


}
