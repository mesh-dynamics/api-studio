package com.cubeiosample.webservices.thirft.thirft;

import java.util.Optional;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class MIRestClient {

    public static void main(String[] args) {
        TTransport transport = new TSocket("localhost", 81);
        try {
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            MIRest.Client client = new MIRest.Client(protocol);

            ListMovieResult result = client.listMovies("ANACONDA CONFESSIONS" , null , null);
//
//
//            //ListStoreResult result = client.listStores(23);
//
            Optional.ofNullable(result.movieInfoList).ifPresent(storeInfoList
                        -> storeInfoList.forEach(x -> System.out.println(x.timestamp)));

            System.out.println(client.healthCheck());
            transport.close();
        } catch (TException e) {
            e.printStackTrace();
        }


    }


}
