package com.cube.core;

import com.cube.cache.TemplateKey.Type;
import com.cube.utils.Constants;

import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;

import redis.clients.jedis.Jedis;

import com.cube.cache.TemplateKey;

public class JedisTest {

    public static void main(String[] args) {
        /*String path = "http://www.abc.com/a/b/c?filName=blah";
        try {
            URIBuilder uriBuilder = new URIBuilder(path);
            System.out.println((uriBuilder.getPath()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }*/


        /*Jedis jedis = new Jedis("localhost" , 6379);
        String pingReply = jedis.ping();
        System.out.println(pingReply);
        TemplateKey key = new TemplateKey(Constants.DEFAULT_TEMPLATE_VER,"ravivj" , "movieinfo" , "movieinfo"
            , "/hello/world" , Type.RequestMatch);
        //jedis.set(key.toString() , "amazing");

        System.out.println(jedis.get(key.toString()));*/
    }

}
