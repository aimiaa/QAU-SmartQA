package com.aimi.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户登录VO")
public class UserLoginVO {

    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "jwt令牌")
    private String token;
}
