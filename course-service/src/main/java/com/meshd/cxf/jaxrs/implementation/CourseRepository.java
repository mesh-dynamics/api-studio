package com.meshd.cxf.jaxrs.implementation;

import java.io.IOException;
import java.util.ArrayList;
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
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.interceptor.apachecxf.egress.ClientFilter;
import com.cube.interceptor.apachecxf.egress.TracingFilter;

@Path("meshd")
@Produces("application/json")
public class CourseRepository {
    private Map<Integer, Course> courses = new HashMap<>();
    private String URL = "http://34.220.106.159:8080/meshd/students/1?source=aaa&trial=bbb";

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
    public Student findStudentById(@PathParam("studentId") int id) {
//        WebClient studentWebClient = webClient.path(URL + id);
        WebClient studentWebClient = WebClient.create(URL+id, List.of(new ClientFilter(), new TracingFilter())).accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
            javax.ws.rs.core.MediaType.APPLICATION_JSON);
//        WebClient studentWebClient = webClient;

//        config.getInInterceptors().add(new LoggingInInterceptor());
//        config.getOutInterceptors().add(new LoggingOutInterceptor());
        Response response = studentWebClient.get();
        int code = response.getStatus();
        if (code >= 200 && code <= 299) {
            ObjectMapper objectMapper = new ObjectMapper();
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
}
