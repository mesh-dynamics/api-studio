package com.meshd.cxf.jaxrs.implementation;

import static io.cube.apachecxf.egress.Utils.getMockingURI;

import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.apachecxf.egress.MDClientLoggingFilter;
import io.cube.apachecxf.egress.MDClientMockingFilter;
import io.cube.apachecxf.egress.MDClientTracingFilter;


@XmlRootElement(name = "Course")
public class Course {
    private int id;
    private String name;
    private List<Integer> studentIds = new ArrayList<>();
    private String BASE_URL = System.getenv("student.service.url");
    private String URL = BASE_URL!=null ? BASE_URL + "/meshd/students?source=aaa&trial=bbb" :
        "http://34.220.106.159:8080/meshd/students?source=aaa&trial=bbb";
    //    private String URL = "http://34.220.106.159:8080/meshd/students?source=aaa&trial=bbb";
    private WebClient webClient = WebClient.create(URL, Arrays
        .asList(new MDClientLoggingFilter(), new MDClientMockingFilter(), new MDClientTracingFilter()), true).accept(javax.ws.rs.core.MediaType.APPLICATION_JSON).type(
        javax.ws.rs.core.MediaType.APPLICATION_JSON);

    private Logger logger = LoggerFactory.getLogger(Course.class);

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
        WebClient localWebClient = WebClient.fromClient(webClient).create(getMockingURI(webClient.getBaseURI().toString()), Arrays
            .asList(new MDClientLoggingFilter(), new MDClientMockingFilter(), new MDClientTracingFilter()));
        Response response = localWebClient.type(MediaType.APPLICATION_JSON).post(objectMapper.writeValueAsString(student));
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

        logger.info("Student to be delete is found :" + studentId);

        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.setPath(uriBuilder.getPath()+"/"+studentId);

        logger.info("Student delete call to be sent  :" + uriBuilder.getPath());
        WebClient studentWebClient = webClient.path(uriBuilder.build().toString());

        Response response = studentWebClient.delete();
        logger.info("Student delete call response  :" + response.getStatus());

        int code = response.getStatus();

        return Response.status(code).build();
    }


    private Student findById(int id) throws Exception {

        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.setPath(uriBuilder.getPath()+"/"+id);
        logger.info("Sending call to student service :" + uriBuilder.toString());
        WebClient studentWebClient = webClient.path(uriBuilder.build().toString());

        Response response = studentWebClient.get();

        logger.info("Response status from student service " + response.getStatus());
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