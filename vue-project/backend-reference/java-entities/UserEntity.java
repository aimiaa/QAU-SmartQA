package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 系统用户：学生、教师、管理员账号。 */
@Data
@TableName("user_account")
public class UserEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String username;
  private String realName;
  private String passwordHash;
  private String roleCode;
  private String department;
  private String email;
  private String phone;
  private String status;
  private LocalDateTime lastLoginAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
