package com.cube.core;


import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Class to test endpoints for analysis service
 */
public class AnalyzeServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(AnalyzeServiceTest.class);

    private static String cubeServeUrl = "http://localhost:8080/cubews_war_exploded/";
    private static String analyzePath = "as/health";
    private static String storeTemplatePath = "as/registerTemplate/response/{customerId}/{appId}/{serviceName}/{path}";

    //private static String

    private HttpClient httpClient;

    @BeforeEach
    public  void before(){
        httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.useragent", "Test Client");
    }


    /**
     * Method to check the health of analysis service
     * TODO change system out methods to logical assert statements
     *
     */
    public void testAnalyzeServiceHealth() {
        GetMethod getMethod = new GetMethod(cubeServeUrl.concat(analyzePath));
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: " + getMethod.getStatusLine());
            }

            // Read the response body.
            byte[] responseBody = getMethod.getResponseBody();

            // Deal with the response.
            System.out.println(new String(responseBody));
        } catch (HttpException e) {

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
    }


    /**
     * Simple Test to store an analysis template for a combination of customerId, appId, serviceName, path
     * TODO change system out statements to logical assert statements
     */

    public void testCreateCompareTemplateService() {

        String storeTemplateSubpath = storeTemplatePath.replace("{appId}" , "bookinfo")
                .replace("{customerId}","1234").replace("{serviceName}" , "getAllBooks")
                .replace("{path}" , "/new/path");
        PostMethod postMethod = new PostMethod(cubeServeUrl.concat(storeTemplateSubpath));
        CompareTemplate template = new CompareTemplate("");
        String[] paths = {"", "/string", "/int", "/float", "/obj", "/rptArr", "/nrptArr"};
        CompareTemplate.DataType[] dataTypes = {CompareTemplate.DataType.Obj, CompareTemplate.DataType.Str, CompareTemplate.DataType.Int, CompareTemplate.DataType.Float, CompareTemplate.DataType.Obj,
                CompareTemplate.DataType.RptArray, CompareTemplate.DataType.NrptArray};

        Map<String, TemplateEntry> templateEntryMap = new HashMap<>();

        for (int i = 0; i < paths.length; i++) {
            TemplateEntry rule = new TemplateEntry(paths[i], dataTypes[i], CompareTemplate.PresenceType.Required,
                    CompareTemplate.ComparisonType.Ignore);
            templateEntryMap.put(paths[i], rule);
            template.addRule(rule);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());



        try {
            final String JSON_STRING = mapper.writeValueAsString(template);
            postMethod.setRequestEntity(new RequestEntity() {
                @Override
                public boolean isRepeatable() {
                    return false;
                }

                @Override
                public void writeRequest(OutputStream outputStream) throws IOException {
                    outputStream.write(JSON_STRING.getBytes());
                }

                @Override
                public long getContentLength() {
                    return JSON_STRING.length();
                }

                @Override
                public String getContentType() {
                    return ContentType.APPLICATION_JSON.toString();
                }
            });

            int statusCode = httpClient.executeMethod(postMethod);
            byte[] responseBody = postMethod.getResponseBody();

            // Deal with the response.
             System.out.println(new String(responseBody));
        } catch (IOException e) {
            e.printStackTrace();
        }





    }

}
