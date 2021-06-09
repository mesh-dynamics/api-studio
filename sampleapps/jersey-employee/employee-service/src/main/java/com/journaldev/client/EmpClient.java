package com.journaldev.client;

import javax.ws.rs.core.MediaType;

import com.journaldev.model.EmpRequest;
import com.journaldev.model.ErrorResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class EmpClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String uri = "http://localhost:8084/jersey_1_19_sampleapp_war/emp/emp/getEmp";
		//String uri = "http://localhost:8080/emp/emp/getEmp";
		//String uri = "http://35.160.68.101:8080/emp/emp/getEmp";
		//String uri = "http://34.220.106.159:8080/emp/emp/getEmp";
		//String uri = "http://192.168.1.121:8080/emp/emp/getEmp";
		EmpRequest request = new EmpRequest();
		// set id as 1 for OK response
		request.setId(1);
		request.setName("PK");
		try {
			Client client = Client.create();
			WebResource r = client.resource(uri);
			//ClientResponse response = r.type(MediaType.APPLICATION_XML).post(ClientResponse.class, request);
			ClientResponse response = r.type(MediaType.APPLICATION_XML).header("md-trace-id", "171f7aa0a99fc11%3A171f7aa0a99fc11%3A0%3A1")
					.header("mdctx-md-parent-span", "51795083392da6fd").post(ClientResponse.class, request);
			System.out.println(response.getStatus());
			if (response.getStatus() == 200) {
				String empResponse = response.getEntity(String.class);
				//System.out.println(empResponse.getId() + "::" + empResponse.getName());
				System.out.println("Response string : " + empResponse);
			} else {
				ErrorResponse exc = response.getEntity(ErrorResponse.class);
				System.out.println(exc.getErrorCode());
				System.out.println(exc.getErrorId());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
