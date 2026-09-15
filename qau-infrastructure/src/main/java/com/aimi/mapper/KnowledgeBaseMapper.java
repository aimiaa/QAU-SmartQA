package com.aimi.mapper;

import com.aimi.entity.KnowledgeBaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {

    /**
     * 按条件查询知识库列表，动态 SQL 见 mapper/KnowledgeBaseMapper.xml。
     * 自定义 SQL 不会自动追加逻辑删除条件，XML 内已显式过滤 deleted。
     *
     * @param keyword       关键词，模糊匹配名称、分类、描述，为空不过滤
     * @param status        状态精确匹配，为空不过滤
     * @param category      分类精确匹配，为空不过滤
     * @param excludeStatus 需要排除的状态，为空不排除
     * @return 按更新时间倒序的知识库列表
     */
    List<KnowledgeBaseEntity> selectKnowledgeBaseList(@Param("keyword") String keyword,
                                                      @Param("status") String status,
                                                      @Param("category") String category,
                                                      @Param("excludeStatus") String excludeStatus);
}
