package com.aimi.mapper;

import com.aimi.entity.KnowledgeSyncJobEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeSyncJobMapper extends BaseMapper<KnowledgeSyncJobEntity> {

    @Select("""
            SELECT * FROM knowledge_sync_job
            WHERE status IN ('pending', 'failed')
              AND next_run_at <= CURRENT_TIMESTAMP
            ORDER BY created_at, id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<KnowledgeSyncJobEntity> selectClaimable(@Param("limit") int limit);

    @Update("""
            UPDATE knowledge_sync_job
            SET status = 'running',
                attempts = attempts + 1,
                started_at = CURRENT_TIMESTAMP,
                finished_at = NULL,
                lease_until = CURRENT_TIMESTAMP + (#{leaseSeconds} * INTERVAL '1 second'),
                worker_id = #{workerId},
                error_message = NULL,
                last_error_type = NULL,
                last_error_message = NULL,
                progress = 0
            WHERE id = #{jobId} AND status IN ('pending', 'failed')
            """)
    int claim(@Param("jobId") Long jobId,
              @Param("workerId") String workerId,
              @Param("leaseSeconds") long leaseSeconds);

    @Update("""
            UPDATE knowledge_sync_job
            SET lease_until = CURRENT_TIMESTAMP + (#{leaseSeconds} * INTERVAL '1 second')
            WHERE id = #{jobId} AND status = 'running' AND worker_id = #{workerId}
            """)
    int renewLease(@Param("jobId") Long jobId,
                   @Param("workerId") String workerId,
                   @Param("leaseSeconds") long leaseSeconds);

    @Select("""
            SELECT COUNT(*) FROM knowledge_sync_job
            WHERE id = #{jobId} AND status = 'running'
              AND worker_id = #{workerId} AND lease_until > CURRENT_TIMESTAMP
            """)
    int hasValidLease(@Param("jobId") Long jobId, @Param("workerId") String workerId);

    @Update("""
            UPDATE knowledge_sync_job
            SET status = 'success', progress = 100, finished_at = CURRENT_TIMESTAMP,
                lease_until = NULL, worker_id = NULL,
                error_message = NULL, last_error_type = NULL, last_error_message = NULL
            WHERE id = #{jobId} AND status = 'running' AND worker_id = #{workerId}
              AND lease_until > CURRENT_TIMESTAMP
            """)
    int markSuccess(@Param("jobId") Long jobId, @Param("workerId") String workerId);

    @Update("""
            UPDATE knowledge_sync_job
            SET status = #{status}, progress = 0, finished_at =
                CASE WHEN #{terminal} THEN CURRENT_TIMESTAMP ELSE NULL END,
                next_run_at = #{nextRunAt}, lease_until = NULL, worker_id = NULL,
                error_message = #{errorMessage}, last_error_type = #{errorType},
                last_error_message = #{errorMessage}
            WHERE id = #{jobId} AND status = 'running' AND worker_id = #{workerId}
              AND lease_until > CURRENT_TIMESTAMP
            """)
    int markFailure(@Param("jobId") Long jobId,
                    @Param("workerId") String workerId,
                    @Param("status") String status,
                    @Param("terminal") boolean terminal,
                    @Param("nextRunAt") LocalDateTime nextRunAt,
                    @Param("errorType") String errorType,
                    @Param("errorMessage") String errorMessage);

    @Update("""
            UPDATE knowledge_sync_job
            SET status = 'pending', worker_id = NULL, lease_until = NULL,
                next_run_at = CURRENT_TIMESTAMP
            WHERE status = 'running' AND lease_until < CURRENT_TIMESTAMP
            """)
    int recoverExpired();

    @Update("""
            UPDATE knowledge_sync_job
            SET status = 'cancelled', finished_at = CURRENT_TIMESTAMP,
                lease_until = NULL, worker_id = NULL,
                last_error_type = 'DOCUMENT_DELETED',
                last_error_message = '文档已删除，任务取消'
            WHERE document_id = #{documentId}
              AND status IN ('pending', 'running', 'failed')
            """)
    int cancelActiveByDocumentId(@Param("documentId") Long documentId);

    @Select("""
            SELECT * FROM knowledge_sync_job
            WHERE status = 'dead'
            ORDER BY finished_at DESC NULLS LAST, id DESC
            LIMIT #{limit}
            """)
    List<KnowledgeSyncJobEntity> selectDead(@Param("limit") int limit);

    @Update("""
            UPDATE knowledge_sync_job
            SET status = 'pending', attempts = 0, max_attempts = #{maxAttempts},
                progress = 0, next_run_at = CURRENT_TIMESTAMP,
                started_at = NULL, finished_at = NULL, lease_until = NULL, worker_id = NULL,
                error_message = NULL, last_error_type = NULL, last_error_message = NULL
            WHERE id = #{jobId} AND status = 'dead'
            """)
    int retryDead(@Param("jobId") Long jobId, @Param("maxAttempts") int maxAttempts);
}
