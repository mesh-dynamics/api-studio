package com.cubeui.readJson;

import com.cubeui.readJson.dataModel.instances.AppData;
import com.cubeui.readJson.dataModel.instances.AppInstanceData;
import com.cubeui.readJson.dataModel.instances.InstanceData;
import com.cubeui.readJson.dataModel.instances.Instances;
import com.cubeui.utils.FetchResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import java.util.Collection;

import com.cubeui.readJson.dataModel.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import static org.springframework.http.ResponseEntity.status;

public class ReadJson {

    public static void main(String[] args)  throws  Exception {
        String url;
        String path;
        String instancePath = "";
        String instanceFile;

        if (args.length != 3 ) {
            System.out.println("Enter the environment url e.g https://demo.dev.cubecorp.io");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            url = reader.readLine();

            System.out.println("Enter the customer json file path");
            reader = new BufferedReader(new InputStreamReader(System.in));

            path = reader.readLine();

            System.out.println("Do you want to enter instance file: yes/no");
            reader = new BufferedReader(new InputStreamReader(System.in));
            instanceFile = reader.readLine();

            if (instanceFile.equalsIgnoreCase("yes")) {
                System.out.println("Enter the Instance json file path");
                reader = new BufferedReader(new InputStreamReader(System.in));
                instancePath = reader.readLine();
            }


        } else {
            url = args[0];
            path = args[1];
            instancePath = args[2];
            instanceFile = "yes";
        }
        ReadJson readJson= new ReadJson();
        try {
            JSONObject json = new JSONObject();
            json.put("username", "admin@meshdynamics.io");
            json.put("password", "admin");
            ResponseEntity login = FetchResponse.fetchResponse(url+"/api/login", HttpMethod.POST, "",Optional.of(json));

            String access_token = FetchResponse.getDataField(login,"access_token").toString();
            String token = "Bearer " + access_token;

            ObjectMapper  mapper = new ObjectMapper();
            JsonData data = mapper.readValue(new File(path), JsonData.class);
            AppInstanceData appInstanceData = null;
            if (instanceFile.equalsIgnoreCase("yes")) {
                 appInstanceData = mapper
                    .readValue(new File(instancePath), AppInstanceData.class);
            }

            for(Customers customer: data.getCustomers()){
                JSONObject body =  readJson.createCustomer(customer);
                ResponseEntity response = FetchResponse.fetchResponse(url+"/api/customer/save", HttpMethod.POST, token,Optional.of(body));
                int customerId =  Integer.parseInt(FetchResponse.getDataField(response,"id").toString());
                body = readJson.createJiraCustomer(customer.getJiraCredentials(), customerId);
                FetchResponse.fetchResponse(url+"/api/jira/customer", HttpMethod.POST, token, Optional.of(body));
                Map<Integer, List<Integer>> instanceMap = new HashMap<>();
                List<Integer> appIds = new ArrayList<>();
                for(Apps app: customer.getApps())
                {
                    body = readJson.createApp(app,customerId);
                    response = FetchResponse.fetchResponse(url+"/api/app", HttpMethod.POST, token, Optional.of(body));
                    int appId = Integer.parseInt(FetchResponse.getDataField(response,"id").toString());
                    appIds.add(appId);
                    List<Integer> instanceIds = new ArrayList<>();
                    if(appInstanceData != null) {
                        List<Instances> instances = readJson
                            .getInstances(appInstanceData.getInstanceData(),
                                customer.getName(), app.getName());
                        for (Instances instance : instances) {
                            body = readJson.createInstance(instance, appId);
                            response = FetchResponse
                                .fetchResponse(url + "/api/instance", HttpMethod.POST, token, Optional.of(body));
                            int instanceId = Integer
                                .parseInt(FetchResponse.getDataField(response, "id").toString());
                            instanceIds.add(instanceId);
                        }
                    }
                    instanceMap.put(appId, instanceIds);
                    Map<String, Integer> servicesMap = new HashMap<>();
                    MultiValuedMap<String, Integer> pathMap = new ArrayListValuedHashMap<>();
                    for(ServiceGroups serviceGroup: app.getServiceGroups()){
                        body = readJson.createServiceGroup(serviceGroup,appId);
                        response = FetchResponse.fetchResponse(url+"/api/service-group", HttpMethod.POST, token, Optional.of(body));
                        int serviceGroupId = Integer.parseInt(FetchResponse.getDataField(response,"id").toString());
                        for (Services service: serviceGroup.getServices()) {
                            body = readJson.createService(service,appId,serviceGroupId);
                            response = FetchResponse.fetchResponse(url+"/api/service", HttpMethod.POST, token, Optional.of(body));
                            int serviceId = Integer.parseInt(FetchResponse.getDataField(response,"id").toString());
                            servicesMap.put(service.getName(), serviceId);
                            if(service.getPaths() != null) {
                                for (String paths : service.getPaths()) {
                                    body = readJson.createPath(paths, serviceId);
                                    response = FetchResponse.fetchResponse(url + "/api/path", HttpMethod.POST, token, Optional.of(body));
                                    int pathId = Integer.parseInt(FetchResponse.getDataField(response, "id").toString());
                                    pathMap.put(paths, pathId);
                                }
                            }
                        }
                    }
                    for(TestConfigs testConfig: app.getTestConfigs()) {
                        body = readJson.createTestConfig(testConfig,appId);
                        response = FetchResponse.fetchResponse(url+"/api/test_config", HttpMethod.POST, token, Optional.of(body));
                        int testConfigId = Integer.parseInt(FetchResponse.getDataField(response,"id").toString());
                        for(String service: testConfig.getServices()) {
                            int serviceId = servicesMap.get(service);
                            body = readJson.createTestService(testConfigId, serviceId);
                            response = FetchResponse.fetchResponse(url+"/api/test-service", HttpMethod.POST, token, Optional.of(body));
                        }
                        for(String paths: testConfig.getPaths()) {
                            Collection<Integer> pathIds = pathMap.get(paths);
                            for(Integer pathId: pathIds) {
                                body = readJson.createTestPath(testConfigId, pathId);
                                response = FetchResponse
                                    .fetchResponse(url + "/api/test-path", HttpMethod.POST, token,
                                        Optional.of(body));
                            }
                        }
                        for (String testVirtualizedService: testConfig.getTest_virtualized_services()) {
                            int serviceId = servicesMap.get(testVirtualizedService);
                            body = readJson.createTestService(testConfigId, serviceId);
                            response = FetchResponse.fetchResponse(url + "/api/test_virtualized_service", HttpMethod.POST, token, Optional.of(body));
                        }
                        for (String testIntermediateService: testConfig.getTest_intermediate_services()) {
                            int serviceId = servicesMap.get(testIntermediateService);
                            body = readJson.createTestService(testConfigId, serviceId);
                            response = FetchResponse.fetchResponse(url + "/api/test_intermediate_service", HttpMethod.POST, token, Optional.of(body));
                        }
                    }
                    for(ServiceGraphs serviceGraph: app.getServiceGraphs()) {
                        int fromServiceId = servicesMap.get(serviceGraph.getFrom());
                        int toServiceId = servicesMap.get(serviceGraph.getTo());
                        body = readJson.createServiceGraph(fromServiceId, toServiceId, appId);
                        response = FetchResponse.fetchResponse(url + "/api/service_graph", HttpMethod.POST, token, Optional.of(body));
                    }
                }
                for(Users user: customer.getUsers())
                {
                    try {
                        body = readJson.createUser(user, customerId);
                        response = FetchResponse.fetchResponse(url + "/api/account/create-user", HttpMethod.POST, token, Optional.of(body));
                        if(response.getStatusCode() == HttpStatus.FORBIDDEN)
                        {
                            response = FetchResponse.fetchResponse(url + "/api/account/getUser/"+user.getEmail(), HttpMethod.GET, token, Optional.of(body));
                            int userId = Integer.parseInt(FetchResponse.getDataField(response,"id").toString());
                            for(Integer appId: appIds) {
                                body = readJson.createAppUser(appId,userId);
                                response = FetchResponse.fetchResponse(url + "/api/app-user", HttpMethod.POST, token, Optional.of(body));
                                List<Integer> instanceIds = instanceMap.get(appId);
                                for (Integer instanceId: instanceIds) {
                                    body = readJson.createInstanceUser(instanceId, userId);
                                    response = FetchResponse.fetchResponse(url + "/api/instance-user", HttpMethod.POST, token, Optional.of(body));
                                }
                            }

                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject createCustomer(Customers customer) {
        JSONObject json = new JSONObject();
        json.put("name", customer.getName());
        json.put("email", customer.getEmailId());
        json.put("domainURLs", customer.getDomainUrls());
        return json;
    }

    private JSONObject createJiraCustomer(JiraCredentials jiraCredentials, int customerId) {
        JSONObject json = new JSONObject();
        json.put("userName", jiraCredentials.getUserName());
        json.put("apiKey", jiraCredentials.getApiKey());
        json.put("jiraBaseURL", jiraCredentials.getJiraBaseURL());
        json.put("customerId", customerId);
        return json;
    }

    private JSONObject createApp(Apps app, int customerId) {
        JSONObject json = new JSONObject();
        json.put("name", app.getName());
        json.put("customerId", customerId);
        return json;
    }

    private JSONObject createInstance(Instances instance, int appId) {
        JSONObject json = new JSONObject();
        json.put("name", instance.getName());
        json.put("gatewayEndpoint", instance.getGatewayEndpoint());
        json.put("appId", appId);
        json.put("loggingURL", instance.getLoggingURL());
        return json;
    }

    private JSONObject createServiceGroup(ServiceGroups serviceGroup, int appId) {
        JSONObject json = new JSONObject();
        json.put("name", serviceGroup.getName());
        json.put("appId", appId);
        return json;
    }

    private JSONObject createService(Services service, int appId, int serviceGroupId)
    {
        JSONObject json = new JSONObject();
        json.put("name", service.getName());
        json.put("appId", appId);
        json.put("serviceGroupId", serviceGroupId);
        return json;
    }

    private JSONObject createPath(String path, int serviceId)
    {
        JSONObject json = new JSONObject();
        json.put("path", path);
        json.put("serviceId", serviceId);
        return json;
    }

    private JSONObject createTestConfig(TestConfigs testConfig, int appId) {
        JSONObject json = new JSONObject();
        json.put("testConfigName", testConfig.getTestConfigName());
        json.put("appId", appId);
        return json;
    }

    private JSONObject createTestPath(int testId, int pathId) {
        JSONObject json = new JSONObject();
        json.put("testId", testId);
        json.put("pathId", pathId);
        return json;
    }

    private JSONObject createTestService(int testId, int serviceId) {
        JSONObject json = new JSONObject();
        json.put("testId", testId);
        json.put("serviceId", serviceId);
        return json;
    }

    private JSONObject createServiceGraph(int fromServiceId, int toServiceId, int appId) {
        JSONObject json = new JSONObject();
        json.put("appId", appId);
        json.put("fromServiceId", fromServiceId);
        json.put("toServiceId", toServiceId);
        return json;
    }

    private JSONObject createUser(Users user, int customerId) {
        JSONObject json = new JSONObject();
        json.put("name", user.getName());
        json.put("email", user.getEmail());
        json.put("password", user.getPassword());
        json.put("customerId", customerId);
        json.put("roles", user.getRoles());
        json.put("isActivated", user.isActivated());
        return json;
    }

    private JSONObject createAppUser(int appId, int userId) {
        JSONObject json = new JSONObject();
        json.put("appId", appId);
        json.put("userId", userId);
        return  json;
    }

    private JSONObject createInstanceUser(int instanceId, int userId) {
        JSONObject json = new JSONObject();
        json.put("instanceId", instanceId);
        json.put("userId", userId);
        return  json;
    }

    private List<Instances> getInstances(List<InstanceData> instanceDataList, String customerName, String appName) {
        Optional<InstanceData> instanceData = instanceDataList.stream()
                            .filter(i -> i.getCustomerName().equals(customerName))
                            .findFirst();
        if( instanceData.isPresent()) {
            List<AppData> apps = instanceData.get().getApps();
            Optional<AppData> app = apps.stream().filter(a -> a.getName().equals(appName)).findFirst();
            if (app.isPresent()) {
                return app.get().getInstances();
            }
        }
        return Collections.emptyList();
    }
}
