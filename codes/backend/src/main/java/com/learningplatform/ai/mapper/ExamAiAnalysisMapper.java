package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.ExamAiAnalysis;
import com.learningplatform.ai.domain.ExamAiAnalysisScope;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ExamAiAnalysisMapper {
    String COLUMNS = """
            id, task_id, exam_id, attempt_id, requester_id, analysis_scope,
            report_markdown, input_snapshot_hash, created_at, updated_at
            """;

    @Insert("""
            INSERT INTO ai_exam_analysis (
                task_id, exam_id, attempt_id, requester_id, analysis_scope,
                report_markdown, input_snapshot_hash
            ) VALUES (
                #{taskId}, #{examId}, #{attemptId}, #{requesterId}, #{analysisScope},
                #{reportMarkdown}, #{inputSnapshotHash}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamAiAnalysis analysis);

    @Select("SELECT " + COLUMNS + " FROM ai_exam_analysis WHERE task_id = #{taskId}")
    Optional<ExamAiAnalysis> findByTaskId(Long taskId);

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM ai_exam_analysis
            WHERE exam_id = #{examId}
              AND requester_id = #{requesterId}
              AND analysis_scope = #{scope}
            <if test='attemptId == null'>AND attempt_id IS NULL</if>
            <if test='attemptId != null'>AND attempt_id = #{attemptId}</if>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    List<ExamAiAnalysis> findHistory(
            @Param("examId") Long examId,
            @Param("attemptId") Long attemptId,
            @Param("requesterId") Long requesterId,
            @Param("scope") ExamAiAnalysisScope scope
    );
}
