package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.WrongQuestionAnalysis;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface WrongQuestionAnalysisMapper {
    String COLUMNS = """
            id, task_id, requester_id, exam_count, question_count,
            report_markdown, input_snapshot_hash, created_at, updated_at
            """;

    @Insert("""
            INSERT INTO ai_wrong_question_analysis (
                task_id, requester_id, exam_count, question_count,
                report_markdown, input_snapshot_hash
            ) VALUES (
                #{taskId}, #{requesterId}, #{examCount}, #{questionCount},
                #{reportMarkdown}, #{inputSnapshotHash}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WrongQuestionAnalysis analysis);

    @Select("SELECT " + COLUMNS
            + " FROM ai_wrong_question_analysis WHERE task_id = #{taskId}")
    Optional<WrongQuestionAnalysis> findByTaskId(Long taskId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_wrong_question_analysis
            WHERE requester_id = #{requesterId}
            ORDER BY created_at DESC, id DESC
            LIMIT 20
            """)
    List<WrongQuestionAnalysis> findRecentByRequesterId(Long requesterId);
}
