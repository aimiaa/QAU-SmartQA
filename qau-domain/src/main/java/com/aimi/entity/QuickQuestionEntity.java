package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 快捷问题，对应前端 QuickQuestion。 */
@Data
@TableName("quick_question")
public class QuickQuestionEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 展示标签
    private String label;
    // 问题内容
    private String question;
    // 分类
    private String category;
    // 排序序号（升序展示）
    private Integer sortOrder;
    // 是否启用
    private Boolean enabled;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
