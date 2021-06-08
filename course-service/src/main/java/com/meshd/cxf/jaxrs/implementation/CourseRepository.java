package com.meshd.cxf.jaxrs.implementation;

import static io.cube.apachecxf.egress.Utils.getMockingURI;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.cube.apachecxf.egress.MDClientLoggingFilter;
import io.cube.apachecxf.egress.MDClientMockingFilter;
import io.cube.apachecxf.egress.MDClientTracingFilter;

@Path("meshd")
@Produces("application/json")
public class CourseRepository {

	private Logger LOGGER = LoggerFactory.getLogger(CourseRepository.class);
	ObjectMapper objectMapper = new ObjectMapper();
	private Map<Integer, Course> courses = new HashMap<>();
	private String BASE_URL = System.getenv("student.service.url");
	private String URL = BASE_URL != null ? BASE_URL + "/meshd/students?source=aaa&trial=bbb" :
		"http://34.220.106.159:8080/meshd/students?source=aaa&trial=bbb";
//    private String URL = "http://34.220.106.159:8080/meshd/students?source=aaa&trial=bbb";

	{
		List<Integer> studentIds = new ArrayList<>();
		studentIds.add(1);
		studentIds.add(2);
		studentIds.add(2);

		Course course1 = new Course();
		Course course2 = new Course();
		course1.setId(1);
		course1.setName("REST with Spring");
		course1.setStudents(studentIds);
		course2.setId(2);
		course2.setName("Learn Spring Security");

		courses.put(1, course1);
		courses.put(2, course2);
	}

	@GET
	@Path("courses/{courseId}")
	public Course getCourse(@PathParam("courseId") int courseId) {
		return findById(courseId);
	}

	@PUT
	@Path("courses/{courseId}")
	public Response updateCourse(@PathParam("courseId") int courseId, Course course) {
		Course existingCourse = findById(courseId);
		if (existingCourse == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		if (existingCourse.equals(course)) {
			return Response.notModified().build();
		}
		courses.put(courseId, course);
		return Response.ok().build();
	}

//    @POST
//    @Path("courses")
//    public Response addSCourse(@FormParam("name") String name) {
//
//        Course course = new Course();
//        course.setName("name");
//        course.setId(courses.size() + 1);
//        courses.put(course.getId(), course);
//
//        return Response.ok(course).build();
//    }

	@POST
	@Path("courses/{courseId}/student")
	public Response addStudent(@PathParam("courseId") int courseId, Student student) {
		Course existingCourse = findById(courseId);
		if (existingCourse == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		try {
			return existingCourse.createStudent(student);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.serverError().build();
	}

	@Path("courses/{courseId}/students")
	public Course pathToStudent(@PathParam("courseId") int courseId) {
		return findById(courseId);
	}

	private Course findById(int id) {
		for (Map.Entry<Integer, Course> course : courses.entrySet()) {
			if (course.getKey() == id) {
				return course.getValue();
			}
		}
		return null;
	}

	@GET
	@Path("courses/students/{studentId}")
	public Student findStudentById(@PathParam("studentId") int id) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder(URL);
		uriBuilder.setPath(uriBuilder.getPath() + "/" + id);
		WebClient studentWebClient = WebClient.create(getMockingURI(uriBuilder.build().toString()),
			Arrays.asList(new MDClientLoggingFilter(), new MDClientTracingFilter(),
				new MDClientMockingFilter()))
			.accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
				javax.ws.rs.core.MediaType.APPLICATION_JSON);

		Response response = studentWebClient.get();
		int code = response.getStatus();
		if (code >= 200 && code <= 299) {
			String studentString = response.readEntity(String.class);
			Student student = null;
			try {
				student = objectMapper.readValue(studentString, Student.class);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return student;
//                return objectMapper.readValue(response.body().string(), Student.class);
		}
		return null;
	}

	@GET
	@Path("/dummyCourseList")
	public Course[] dummyCourseList(@QueryParam("count") int courseCount,
		@QueryParam("changeNameCount") int changeNameCount, @QueryParam("changeAll") boolean changeAll) {
		if(changeNameCount > courseCount || changeNameCount > 100) return new Course[1];
		Course[] courseArray = new Course[courseCount];
		Course course = new Course();
		course.setId(courseCount);
		String name = changeAll ?  UUID.randomUUID().toString() : "Dummy Course";
		course.setName(name);
		for (int i = 0; i < courseCount; i++) {
			courseArray[i] = course;
		}

		//Change name count is assumed to be a lower number ideally less than 20
		// Because we're creating that many objects.
		if(changeAll==false && changeNameCount!=0) {
			for (int i=0; i< changeNameCount; i++ ) {
				int randInd = (int)(Math.random() * (courseCount));
				Course courseChange = new Course();
				courseChange.setId(courseCount);
				courseChange.setName(UUID.randomUUID().toString());
				courseArray[randInd] = courseChange;
			}
		}
		return courseArray;
	}

	@GET
	@Path("/dummyStudentList")
	public Response dummyStudentList(@QueryParam("count") int studentCount)
		throws URISyntaxException {
		LOGGER.info("Received called to course/dummyStudentList");
		URIBuilder uriBuilder = new URIBuilder(URL);
		uriBuilder.setPath(uriBuilder.getPath() + "/dummyStudentList");
		uriBuilder.addParameter("count", String.valueOf(studentCount));
		WebClient studentWebClient = WebClient.create(getMockingURI(uriBuilder.build().toString()), Arrays
			.asList(new MDClientLoggingFilter(), new MDClientMockingFilter(), new MDClientTracingFilter()))
			.accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
				javax.ws.rs.core.MediaType.APPLICATION_JSON);

		HTTPConduit http = WebClient.getConfig(studentWebClient).getHttpConduit();
		HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
		httpClientPolicy.setConnectionTimeout(0);
		http.setClient(httpClientPolicy);

		Response response = studentWebClient.get();
		LOGGER.info("Recieved response from student/dummyStudentList. \nStatus" + response.getStatus() + "\nResponse: " + response.toString());
		return response;
	}


	@POST
	@Path("/createStudentNew")
	public Response createStudent(Student student) throws Exception     {

		ObjectMapper objectMapper = new ObjectMapper();
		WebClient localWebClient = WebClient.create(getMockingURI(URL), Arrays
			.asList(new MDClientLoggingFilter(), new MDClientMockingFilter(), new MDClientTracingFilter()));
		Response response = localWebClient.type(MediaType.APPLICATION_JSON).post(objectMapper.writeValueAsString(student));
		int responseCode = response.getStatus();
		if (responseCode >= 200 && responseCode <= 299) {
			return Response.ok(student).build();
		} else {
			throw new IllegalArgumentException(
				"HTTP error response returned by Transformer service " + responseCode);
		}
	}

	@GET
	@Path("/echo")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response echo(JsonNode body, @Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			MultivaluedMap<String, String> headers = httpHeaders.getRequestHeaders();
			URI url = uriInfo.getRequestUri();

//       ((ObjectNode)body).put("Kuch kuch", "NAHI hota hai");
			Map jsonMap = new HashMap();
			jsonMap.put("queryParams", queryParams);
			jsonMap.put("headers", headers);
			jsonMap.put("url", url);
			jsonMap.put("body", body);
			return Response.ok().entity(objectMapper.writeValueAsString(jsonMap)).build();
		} catch (Exception e) {
			return Response.serverError().entity("Error while returning echo info" + e.getMessage())
				.build();
		}
	}

	
	@GET
	@Path("/echoPathParam/{pp1}/dummy1/{pp2}/dummy2/{pp3}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response echo(JsonNode body, @Context UriInfo uriInfo, @Context HttpHeaders httpHeaders,
		@PathParam("pp1") int id, @PathParam("pp2") String pp2, @PathParam("pp2") String pp3) {
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			MultivaluedMap<String, String> headers = httpHeaders.getRequestHeaders();
			MultivaluedMap<String, String> pathParams = uriInfo.getPathParameters();
			URI url = uriInfo.getRequestUri();

//       ((ObjectNode)body).put("Kuch kuch", "NAHI hota hai");
			Map jsonMap = new HashMap();
			jsonMap.put("queryParams", queryParams);
			jsonMap.put("headers", headers);
			jsonMap.put("url", url);
			jsonMap.put("body", body);
			jsonMap.put("pathParams", pathParams);
			return Response.ok().entity(objectMapper.writeValueAsString(jsonMap)).build();
		} catch (Exception e) {
			return Response.serverError().entity("Error while returning echo info" + e.getMessage())
				.build();
		}
	}
}
