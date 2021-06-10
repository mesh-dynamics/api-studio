package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Users {
    String name;
    String email;
    String password;
    List<String> roles;
    boolean activated;
}
