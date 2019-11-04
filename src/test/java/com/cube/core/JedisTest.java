package com.cube.core;

import com.cube.utils.Constants;
import java.util.Optional;

import redis.clients.jedis.Jedis;

import com.cube.cache.TemplateKey;

public class JedisTest {

    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost" , 6379);
        String pingReply = jedis.ping();
        System.out.println(pingReply);
        TemplateKey key = new TemplateKey(Constants.DEFAULT_TEMPLATE_VER,"ravivj" , "movieinfo" , "movieinfo"
            , "/hello/world" , TemplateKey.Type.Request);
        //jedis.set(key.toString() , "amazing");

        System.out.println(jedis.get(key.toString()));
    }

}
