package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserDTO {

    private Long id;

    private String name;

    private String email;

    private String password;

    private List<String> roles = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean isActivated;

//    private boolean isAccountNonExpired;
//
//    private boolean isAccountNonLocked;
//
//    private boolean isCredentialsNonExpired;
//
//    private boolean isEnabled;
}
