package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDTO {

    private String oldPassword;

    private String newPassword;

}
