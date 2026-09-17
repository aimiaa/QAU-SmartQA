package com.aimi.mapper;

import com.aimi.entity.DocumentChunkEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkEntity> {

    /** 批量写入切片，embedding 需 ::vector 转换，SQL 见 mapper/DocumentChunkMapper.xml。 */
    int insertBatch(@Param("chunks") List<DocumentChunkEntity> chunks);

    /** 重解析前物理清理旧切片（切片表不做逻辑删除，向量数据留着无意义）。 */
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
