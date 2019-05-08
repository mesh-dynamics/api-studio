package com.cubeui.backend.domain;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Role {
    ROLE_USER, ROLE_ADMIN;

    public static Set<String> getAllRoles() {
        return Stream.of(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());
    }
}
