package com.aimi.entity;

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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 登录用户名（全局唯一）
    private String username;
    // 真实姓名
    private String realName;
    // 密码哈希（BCrypt 加密存储，禁止保存明文）
    private String passwordHash;
    // 角色编码：student 学生 / teacher 教师 / admin 管理员
    private String roleCode;
    // 所属院系
    private String department;
    // 邮箱
    private String email;
    // 手机号
    private String phone;
    // 账号状态：active 正常 / disabled 停用 / locked 锁定
    private String status;
    // 最后登录时间
    private LocalDateTime lastLoginAt;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
