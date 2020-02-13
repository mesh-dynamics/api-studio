package com.meshd.cxf.jaxrs.implementation;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@XmlRootElement(name = "Course")
public class Course {
    private int id;
    private String name;
    private List<Integer> studentIds = new ArrayList<>();
    private String URL = "http://localhost:8081/meshd/students/";
    private OkHttpClient httpClient = new OkHttpClient();

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
        Request.Builder requestBuilder = new Request.Builder().url(URL);
        requestBuilder.post(okhttp3.RequestBody.create(MediaType.parse("application/json"),
            objectMapper.writeValueAsString(student)));

        try (okhttp3.Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            int code = response.code();
            if (code >= 200 && code <= 299) {
                return Response.ok(student).build();
            } else {
                throw new IllegalArgumentException(
                    "HTTP error response returned by Transformer service " + code);
            }
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