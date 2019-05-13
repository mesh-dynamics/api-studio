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

    private boolean isAccountNonExpired;

    private boolean isAccountNonLocked;

    private boolean isCredentialsNonExpired;

    private boolean isEnabled;

//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public List<String> getRoles() {
//        return roles;
//    }
//
//    public void setRoles(List<String> roles) {
//        this.roles = roles;
//    }
//
//    public boolean isAccountNonExpired() {
//        return isAccountNonExpired;
//    }
//
//    public void setAccountNonExpired(boolean accountNonExpired) {
//        isAccountNonExpired = accountNonExpired;
//    }
//
//    public boolean isAccountNonLocked() {
//        return isAccountNonLocked;
//    }
//
//    public void setAccountNonLocked(boolean accountNonLocked) {
//        isAccountNonLocked = accountNonLocked;
//    }
//
//    public boolean isCredentialsNonExpired() {
//        return isCredentialsNonExpired;
//    }
//
//    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
//        isCredentialsNonExpired = credentialsNonExpired;
//    }
//
//    public boolean isEnabled() {
//        return isEnabled;
//    }
//
//    public void setEnabled(boolean enabled) {
//        isEnabled = enabled;
//    }
}
