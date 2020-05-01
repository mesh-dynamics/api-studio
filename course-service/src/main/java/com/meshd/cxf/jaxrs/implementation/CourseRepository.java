package com.meshd.cxf.jaxrs.implementation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.interceptor.apachecxf.egress.ClientFilter;
import io.cube.interceptor.apachecxf.egress.TracingFilter;

@Path("meshd")
@Produces("application/json")
public class CourseRepository {
    ObjectMapper objectMapper = new ObjectMapper();
    private Map<Integer, Course> courses = new HashMap<>();
    private String BASE_URL = System.getenv("student.service.url");
    private String URL = BASE_URL!=null ? BASE_URL + "/meshd/students?source=aaa&trial=bbb" :
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
//        WebClient studentWebClient = webClient.path(URL + id);
        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.setPath(uriBuilder.getPath()+"/"+id);
        WebClient studentWebClient = WebClient.create(uriBuilder.build().toString(), Arrays.asList(new ClientFilter(), new TracingFilter())).accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
            javax.ws.rs.core.MediaType.APPLICATION_JSON);
//        WebClient studentWebClient = webClient;

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
    public List<Course> dummyCourseList(@QueryParam("count") int courseCount) {
        List<Course> courseList = new ArrayList<>();
        Course course = new Course();
        for (int i=0; i<courseCount; i++) {
            course.setId(courseCount);
            course.setName("Dummy Course");
//            List<Integer> studentIds = new ArrayList<>();
//            studentIds.add(i);
//            course.setStudents(studentIds);
            courseList.add(course);
        }
        return courseList;
    }

    @GET
    @Path("/dummyStudentList")
    public Response dummyStudentList(@QueryParam("count") int studentCount)
        throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.setPath(uriBuilder.getPath()+"/dummyStudentList");
        uriBuilder.addParameter("count", String.valueOf(studentCount));
        WebClient studentWebClient = WebClient.create(uriBuilder.build().toString(), Arrays
            .asList(new ClientFilter(), new TracingFilter())).accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
            javax.ws.rs.core.MediaType.APPLICATION_JSON);
//        WebClient studentWebClient = webClient;

        Response response = studentWebClient.get();
        return response;
    }
}
