package com.meshd.cxf.jaxrs.implementation;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.client.utils.URIBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cube.interceptor.apachecxf.egress.ClientFilter;
import com.cube.interceptor.apachecxf.egress.MockingClientFilter;
import com.cube.interceptor.apachecxf.egress.TracingFilter;


@XmlRootElement(name = "Course")
public class Course {
    private int id;
    private String name;
    private List<Integer> studentIds = new ArrayList<>();
    private String BASE_URL = System.getenv("student.service.url");
    private String URL = BASE_URL!=null ? BASE_URL + "/meshd/students?source=aaa&trial=bbb" :
        "http://34.220.106.159:8080/meshd/students?source=aaa&trial=bbb";
    //    private String URL = "http://34.220.106.159:8080/meshd/students?source=aaa&trial=bbb";
    private WebClient webClient = WebClient.create(URL, List.of(new ClientFilter(), new TracingFilter(), new MockingClientFilter()), true).accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
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
//        return Response.ok().build();
        for (Integer id : studentIds) {
            if (id == student.getId()) {
                return Response.status(Response.Status.CONFLICT).build();
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Response response = webClient.type(MediaType.APPLICATION_JSON).post(objectMapper.writeValueAsString(student));
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

        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.setPath(uriBuilder.getPath()+"/"+studentId);
        WebClient studentWebClient = webClient.path(uriBuilder.build().toString());

        Response response = studentWebClient.delete();

        int code = response.getStatus();

        return Response.status(code).build();
    }


    private Student findById(int id) throws Exception {

        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.setPath(uriBuilder.getPath()+"/"+id);
        WebClient studentWebClient = webClient.path(uriBuilder.build().toString());

        ClientConfiguration config = WebClient.getConfig(studentWebClient);
        Response response = studentWebClient.get();

            int code = response.getStatus();
            if (code >= 200 && code <= 299) {
                ObjectMapper objectMapper = new ObjectMapper();
                String studentString = response.readEntity(String.class);
                Student student = objectMapper.readValue(studentString, Student.class);
                return student;
//                return objectMapper.readValue(response.body().string(), Student.class);
            } else if (code == 404 ){
                throw new NotFoundException();
            } else {
                throw new IllegalArgumentException(
                    "HTTP error response returned by Transformer service " + code);
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