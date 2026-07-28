/* 文件职责：定义考试考生的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamCandidate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义考试考生的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamCandidateMapper {
    @Select("""
            SELECT ec.id, ec.exam_id, ec.user_id, u.username, u.nickname,
                   ec.status, ec.assigned_at, ec.started_at, ec.submitted_at,
                   ec.created_at, ec.updated_at
            FROM exam_candidate ec
            JOIN `user` u ON u.id = ec.user_id AND u.deleted = 0
            WHERE ec.exam_id = #{examId}
            ORDER BY ec.id ASC
            """)
    /** 执行 findByExamId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ExamCandidate> findByExamId(Long examId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM exam_candidate
            WHERE exam_id = #{examId} AND user_id = #{userId}
            """)
    /** 执行 exists 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean exists(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT ec.id, ec.exam_id, ec.user_id, u.username, u.nickname,
                   ec.status, ec.assigned_at, ec.started_at, ec.submitted_at,
                   ec.created_at, ec.updated_at
            FROM exam_candidate ec
            JOIN `user` u ON u.id = ec.user_id AND u.deleted = 0
            WHERE ec.exam_id = #{examId} AND ec.user_id = #{userId}
            """)
    /** 执行 findOne 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamCandidate> findOne(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT ec.id, ec.exam_id, ec.user_id, u.username, u.nickname,
                   ec.status, ec.assigned_at, ec.started_at, ec.submitted_at,
                   ec.created_at, ec.updated_at
            FROM exam_candidate ec
            JOIN `user` u ON u.id = ec.user_id AND u.deleted = 0
            WHERE ec.exam_id = #{examId} AND ec.user_id = #{userId}
            FOR UPDATE
            """)
    /** 执行 findOneForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamCandidate> findOneForUpdate(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    @Insert("""
            INSERT INTO exam_candidate (exam_id, user_id, status)
            VALUES (#{examId}, #{userId}, 'ASSIGNED')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(ExamCandidate candidate);

    @Update("""
            UPDATE exam_candidate
            SET status = 'STARTED', started_at = #{startedAt}
            WHERE id = #{candidateId} AND status = 'ASSIGNED'
            """)
    /** 执行 markStarted 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markStarted(
            @Param("candidateId") Long candidateId,
            @Param("startedAt") java.time.LocalDateTime startedAt
    );

    @Update("""
            UPDATE exam_candidate
            SET status = 'SUBMITTED', submitted_at = #{submittedAt}
            WHERE id = #{candidateId} AND status IN ('ASSIGNED', 'STARTED')
            """)
    /** 执行 markSubmitted 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markSubmitted(
            @Param("candidateId") Long candidateId,
            @Param("submittedAt") java.time.LocalDateTime submittedAt
    );

    @Delete("DELETE FROM exam_candidate WHERE exam_id = #{examId}")
    /** 执行 deleteByExamId 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteByExamId(Long examId);
}
