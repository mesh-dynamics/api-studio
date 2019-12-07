package com.cubeiosample.webservices.thrift;

import java.util.Optional;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class MIRestClient {

    public static void main(String[] args) {
        TTransport transport = new TSocket("localhost", 9090);

        try  {
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            MIThrift.Client client = new MIThrift.Client(protocol);

            ListMovieResult result = client.listMovies("ANACONDA CONFESSIONS" , null , null , null);
//
//
//            //ListStoreResult result = client.listStores(23);
//
            Optional.ofNullable(result.movieInfoList).ifPresent(storeInfoList
                        -> storeInfoList.forEach(x -> System.out.println(x.actorsFirstNames.toString())));

            System.out.println(client.healthCheck());
            transport.close();
        } catch (TException e) {
            e.printStackTrace();
        }


    }


}
