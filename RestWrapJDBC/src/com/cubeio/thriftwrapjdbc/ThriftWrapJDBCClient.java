package com.cubeio.thriftwrapjdbc;

import java.util.Map;
import java.util.Optional;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import io.cube.utils.Tracing;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.thrift.internal.reporters.protocols.JaegerThriftSpanConverter;
import io.jaegertracing.thriftjava.Span;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

public class ThriftWrapJDBCClient {

    public static Scope startClientSpan(String operationName) {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).startActive(true);
    }

    public static void main(String[] args) {
        String MYSQL_HOST = "sakila2.cnt3lftdrpew.us-west-2.rds.amazonaws.com";  // "localhost";
        String MYSQL_PORT = "3306";
        String MYSQL_DBNAME = "sakila";
        String MYSQL_USERNAME = "cube";
        String MYSQL_PWD = "cubeio12";  // AWS RDS pwd
        String jdbcHost = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/sakila";

        TTransport transport = new TSocket("localhost", 9091);

        JaegerTracer tracer = Tracing.init("MIThrift");
        GlobalTracer.register(tracer);
        try (Scope scope = startClientSpan("ThriftQuery");) {
            System.out.println("Current Span Is :: " + scope.span().toString());

            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            ThriftWrapJDBC.Client client = new ThriftWrapJDBC.Client(protocol);
            String result = "";

            String queryString = "select film.film_id as film_id, film.title as title, group_concat(actor_film_count.first_name) as actors_firstnames, group_concat(actor_film_count.last_name) as actors_lastnames, group_concat(actor_film_count.film_count) as film_counts from film, film_actor, actor_film_count  where film.film_id = film_actor.film_id and film_actor.actor_id = actor_film_count.actor_id  and title = ? group by film.film_id, film.title";

            JaegerSpan span = (JaegerSpan) scope.span();

            Span thriftSpan = JaegerThriftSpanConverter.convertSpan(span);
            thriftSpan.setBaggage(Map.of("intent", "record"));

            String queryParams = "[{\"index\":1,\"type\":\"string\",\"value\":\"GONE TROUBLE\"}]";
            result = client.query(queryString, queryParams, thriftSpan);
            System.out.println(result);

            transport.close();
        } catch (TException e) {
            e.printStackTrace();
        }


    }

}
