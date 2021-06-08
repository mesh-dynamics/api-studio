package com.baeldung.cxf.jaxrs.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("meshd")
@Produces("application/json")
public class StudentRepository {
  private Map<Integer, Student> students = new HashMap<>();

  {
    Student student1 = new Student();
    Student student2 = new Student();
    student1.setId(1);
    student1.setName("Student A");
    student2.setId(2);
    student2.setName("Student B");
    students.put(1, student1);
    students.put(2, student2);

  }

  @GET
  @Path("students/{studentId}")
  public Student getStudent(@PathParam("studentId") int studentId) {
    Student student = findById(studentId);
    if (student == null) {
      throw new NotFoundException();
    } else {
      return student;
    }
  }

  @POST
  @Path("students")
  public Response createStudent(Student student) {
    students.put(student.getId(), student);
    return Response.ok(student).build();
  }

  @DELETE
  @Path("students/{studentId}")
  public Response deleteStudent (@PathParam("studentId") int studentId) {
    Student student = findById(studentId);
    if (student == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    students.remove(studentId);
    return Response.ok().build();
  }

  private Student findById(int id) {
    for (Map.Entry<Integer, Student> student : students.entrySet()) {
      if (student.getKey() == id) {
        return student.getValue();
      }
    }
    return null;
  }

  @GET
  @Path("students/dummyStudentList")
  public List<Student> dummyStudentList(@QueryParam("count") int studentCount) {
    List<Student> studentList = new ArrayList<>();
    Student student = new Student();
    student.setId(studentCount);
    student.setName("Dummy Student");
    for (int i=0; i<studentCount; i++) {
      studentList.add(student);
    }
    return studentList;
  }
}
