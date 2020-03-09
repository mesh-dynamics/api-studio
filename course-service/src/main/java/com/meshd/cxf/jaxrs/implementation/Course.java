package com.meshd.cxf.jaxrs.implementation;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import com.cube.interceptor.apachecxf.egress.ClientFilter;
import com.cube.interceptor.apachecxf.egress.TracingFilter;


@XmlRootElement(name = "Course")
public class Course {
    private int id;
    private String name;
    private List<Integer> studentIds = new ArrayList<>();
    private String URL = "http://localhost:8081/meshd/students?source=aaa&trial=bbb";
    private OkHttpClient httpClient = new OkHttpClient();
    private WebClient webClient = WebClient.create(URL, List.of(new ClientFilter(), new TracingFilter())).accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
        javax.ws.rs.core.MediaType.APPLICATION_JSON);

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getStudents() {
        return studentIds;
    }

    public void setStudents(List<Integer> studentIds) {
        this.studentIds = studentIds;
    }

    @GET
    @Path("{studentId}")
    public Student getStudent(@PathParam("studentId") int studentId) throws Exception {
        return findById(studentId);
    }

    @POST
    public Response createStudent(Student student) throws Exception     {
        for (Integer id : studentIds) {
            if (id == student.getId()) {
                return Response.status(Response.Status.CONFLICT).build();
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Response response = webClient.post(objectMapper.writeValueAsString(student));
        int responseCode = response.getStatus();
        if (responseCode >= 200 && responseCode <= 299) {
            studentIds.add(student.getId());
            return Response.ok(student).build();
        } else {
            throw new IllegalArgumentException(
                "HTTP error response returned by Transformer service " + responseCode);
        }
    }

    @DELETE
    @Path("{studentId}")
    public Response deleteStudent(@PathParam("studentId") int studentId) throws Exception {
        Student student = findById(studentId);
        if (student == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Request.Builder requestBuilder = new Request.Builder()
            .url(URL+studentId).delete();
        try (okhttp3.Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            int code = response.code();
            return Response.status(code).build();
        }

    }

    private Student findById(int id) throws Exception {
        Request.Builder requestBuilder = new Request.Builder()
            .url(URL+id);

        try (okhttp3.Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            int code = response.code();
            if (code >= 200 && code <= 299) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(response.body().string(), Student.class);
            } else if (code == 404 ){
                throw new NotFoundException();
            } else {
                throw new IllegalArgumentException(
                    "HTTP error response returned by Transformer service " + code);
            }
        }
    }

    @Override
    public int hashCode() {
        return id + name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Course) && (id == ((Course) obj).getId()) && (name.equals(((Course) obj).getName()));
    }
}