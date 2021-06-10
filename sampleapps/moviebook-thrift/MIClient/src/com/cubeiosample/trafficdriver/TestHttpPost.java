package com.cubeiosample.trafficdriver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class TestHttpPost {

    public static void main (String[] args) {
        try {
            List<String> modified1 = Stream.concat(Arrays.asList("a1").stream() , Stream.of("b1")).collect(Collectors.toList());
            List<String> modified2 = Stream.concat(Arrays.asList("a1").stream() , Stream.of("b2")).collect(Collectors.toList());
            System.out.println(modified1);
            System.out.println(modified2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
