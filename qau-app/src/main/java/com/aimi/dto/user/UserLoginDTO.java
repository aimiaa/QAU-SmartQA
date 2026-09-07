package com.aimi.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "用户登录DTO")
public class UserLoginDTO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;
}


