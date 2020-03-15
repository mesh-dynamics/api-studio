package com.cubeui.readJson;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.readJson.dataModel.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.dao.Replay;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.http.ResponseEntity.status;

public class ReadJson {

    private RestTemplate restTemplate = new RestTemplate();

    private ResponseEntity fetchResponse(String path, HttpMethod method, String token, String body) throws Exception{
        ResponseEntity response;
        try {
            URI uri = new URI(path);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", token);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            response = restTemplate.exchange(uri, method, entity, String.class);
            return response;
        } catch (HttpClientErrorException e){
            response = status(e.getStatusCode()).body(new ErrorResponse(e.getLocalizedMessage()));
            return response;
        } catch (Exception e){
            throw e;
        }
    }
    public static void main(String[] args)  throws  Exception {
        String url;
        String path;

        if (args.length != 2 ) {
            System.out.println("Enter the domain url");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            url = reader.readLine();

            System.out.println("Enter the json file path");
            reader = new BufferedReader(new InputStreamReader(System.in));

            path = reader.readLine();
        } else {
            url = args[0];
            path = args[1];
        }
        ReadJson readJson= new ReadJson();
        try {
            ResponseEntity login = readJson.fetchResponse(url+"/api/login", HttpMethod.POST, "","{\"username\":\"admin@meshdynamics.io\",\"password\":\"admin\"}");

            String access_token = readJson.getDataField(login,"access_token").toString();
            String token = "Bearer " + access_token;

            ObjectMapper  mapper = new ObjectMapper();
            JsonData data = mapper.readValue(new File(path), JsonData.class);

            for(Customers customer: data.getCustomers()){
                String body =  readJson.createCustomer(customer);
                ResponseEntity response = readJson.fetchResponse(url+"/api/customer/save", HttpMethod.POST, token,body);
                if (response.getStatusCode() == HttpStatus.FORBIDDEN)
                {
                    response = readJson.fetchResponse(url+"/api/customer/update", HttpMethod.POST, token,body);
                }
                int customerId =  Integer.parseInt(readJson.getDataField(response,"id").toString());
                for(Apps app: customer.getApps())
                {
                    body = readJson.createApp(app,customerId);
                    response = readJson.fetchResponse(url+"/api/app", HttpMethod.POST, token,body);
                    int appId = Integer.parseInt(readJson.getDataField(response,"id").toString());
                    for(Instances instance: app.getInstances()) {
                        body = readJson.createInstance(instance,appId);
                        response = readJson.fetchResponse(url+"/api/instance", HttpMethod.POST, token,body);
                    }
                    Map<String, Integer> servicesMap = new HashMap<>();
                    Map<String, Integer> pathMap = new HashMap<>();
                    for(ServiceGroups serviceGroup: app.getServiceGroups()){
                        body = readJson.createServiceGroup(serviceGroup,appId);
                        response = readJson.fetchResponse(url+"/api/service-group", HttpMethod.POST, token,body);
                        int serviceGroupId = Integer.parseInt(readJson.getDataField(response,"id").toString());
                        for (Services service: serviceGroup.getServices()) {
                            body = readJson.createService(service,appId,serviceGroupId);
                            response = readJson.fetchResponse(url+"/api/service", HttpMethod.POST, token,body);
                            int serviceId = Integer.parseInt(readJson.getDataField(response,"id").toString());
                            servicesMap.put(service.getName(), serviceId);
                            if(service.getPaths() != null) {
                                for (String paths : service.getPaths()) {
                                    body = readJson.createPath(paths, serviceId);
                                    response = readJson.fetchResponse(url + "/api/path", HttpMethod.POST, token, body);
                                    int pathId = Integer.parseInt(readJson.getDataField(response, "id").toString());
                                    pathMap.put(service.getName() + "_" + paths, pathId);
                                }
                            }
                        }
                    }
                    for(TestConfigs testConfig: app.getTestConfigs()) {
                        int serviceId = servicesMap.get(testConfig.getServiceName());
                        body = readJson.createTestConfig(testConfig,appId,serviceId);
                        response = readJson.fetchResponse(url+"/api/test_config", HttpMethod.POST, token,body);
                        int testConfigId = Integer.parseInt(readJson.getDataField(response,"id").toString());
                        for(String paths: testConfig.getPaths()) {
                            int pathId = pathMap.get(testConfig.getServiceName()+"_"+paths);
                            body = readJson.createTestPath(testConfigId,pathId);
                            response = readJson.fetchResponse(url+"/api/test-path", HttpMethod.POST, token,body);
                        }
                        /**
                         * Need to check the Which Services we need to update for the given test
                         * Is it the same as used for path or different
                         */
                        body = readJson.createTestVirtualizedService(testConfigId,serviceId);
                        response = readJson.fetchResponse(url+"/api/test_virtualized_service", HttpMethod.POST, token,body);
                        response = readJson.fetchResponse(url+"/api/test_intermediate_service", HttpMethod.POST, token,body);
                    }
                }
                for(Users user: customer.getUsers())
                {
                    body = readJson.createUser(user,customerId);
                    response = readJson.fetchResponse(url+"/api/account/create-user", HttpMethod.POST, token,body);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Object getDataField(ResponseEntity response, String field) throws ParseException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response.getBody().toString());
            return json.get(field).toString();
        } catch (ParseException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String createCustomer(Customers customer) {
        JSONObject json = new JSONObject();
        json.put("name", customer.getName());
        json.put("email", customer.getEmailId());
        json.put("domainURL", customer.getDomainUrl());
        return json.toString();
    }

    private String createApp(Apps app, int customerId) {
        JSONObject json = new JSONObject();
        json.put("name", app.getName());
        json.put("customerId", customerId);
        return json.toString();
    }

    private String createInstance(Instances instance, int appId) {
        JSONObject json = new JSONObject();
        json.put("name", instance.getName());
        json.put("gatewayEndpoint", instance.getGatewayEndpoint());
        json.put("appId", appId);
        return json.toString();
    }

    private String createServiceGroup(ServiceGroups serviceGroup, int appId) {
        JSONObject json = new JSONObject();
        json.put("name", serviceGroup.getName());
        json.put("appId", appId);
        return json.toString();
    }

    private String createService(Services service, int appId, int serviceGroupId)
    {
        JSONObject json = new JSONObject();
        json.put("name", service.getName());
        json.put("appId", appId);
        json.put("serviceGroupId", serviceGroupId);
        return json.toString();
    }

    private String createPath(String path, int serviceId)
    {
        JSONObject json = new JSONObject();
        json.put("path", path);
        json.put("serviceId", serviceId);
        return json.toString();
    }

    private String createTestConfig(TestConfigs testConfig, int appId, int serviceId) {
        JSONObject json = new JSONObject();
        json.put("testConfigName", testConfig.getTestConfigName());
        json.put("appId", appId);
        json.put("gatewayServiceId", serviceId);
        return json.toString();
    }

    private String createTestPath(int testId, int pathId) {
        JSONObject json = new JSONObject();
        json.put("testId", testId);
        json.put("pathId", pathId);
        return json.toString();
    }

    private String createTestVirtualizedService(int testId, int serviceId) {
        JSONObject json = new JSONObject();
        json.put("testId", testId);
        json.put("serviceId", serviceId);
        return json.toString();
    }

    private String createUser(Users user, int customerId) {
        JSONObject json = new JSONObject();
        json.put("name", user.getName());
        json.put("email", user.getEmail());
        json.put("password", user.getPassword());
        json.put("customerId", customerId);
        json.put("roles", user.getRoles());
        json.put("isActivated", user.isActivated());
        return json.toString();
    }
}
