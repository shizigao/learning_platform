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
    List<ExamCandidate> findByExamId(Long examId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM exam_candidate
            WHERE exam_id = #{examId} AND user_id = #{userId}
            """)
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
    Optional<ExamCandidate> findOneForUpdate(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    @Insert("""
            INSERT INTO exam_candidate (exam_id, user_id, status)
            VALUES (#{examId}, #{userId}, 'ASSIGNED')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamCandidate candidate);

    @Update("""
            UPDATE exam_candidate
            SET status = 'STARTED', started_at = #{startedAt}
            WHERE id = #{candidateId} AND status = 'ASSIGNED'
            """)
    int markStarted(
            @Param("candidateId") Long candidateId,
            @Param("startedAt") java.time.LocalDateTime startedAt
    );

    @Update("""
            UPDATE exam_candidate
            SET status = 'SUBMITTED', submitted_at = #{submittedAt}
            WHERE id = #{candidateId} AND status IN ('ASSIGNED', 'STARTED')
            """)
    int markSubmitted(
            @Param("candidateId") Long candidateId,
            @Param("submittedAt") java.time.LocalDateTime submittedAt
    );

    @Delete("DELETE FROM exam_candidate WHERE exam_id = #{examId}")
    int deleteByExamId(Long examId);
}
