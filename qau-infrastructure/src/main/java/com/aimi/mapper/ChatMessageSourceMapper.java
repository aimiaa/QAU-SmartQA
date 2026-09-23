package com.aimi.mapper;

import com.aimi.entity.ChatMessageSourceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** AI 回复来源表 Mapper：记录回答引用的知识库、文档与切片，用于前端来源标签展示。 */
@Mapper
public interface ChatMessageSourceMapper extends BaseMapper<ChatMessageSourceEntity> {
}