package com.baeldung.cxf.jaxrs.implementation;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("baeldung")
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
  public Student getCourse(@PathParam("studentId") int studentId) {
    return findById(studentId);
  }

  private Student findById(int id) {
    for (Map.Entry<Integer, Student> student : students.entrySet()) {
      if (student.getKey() == id) {
        return student.getValue();
      }
    }
    return null;
  }
}
