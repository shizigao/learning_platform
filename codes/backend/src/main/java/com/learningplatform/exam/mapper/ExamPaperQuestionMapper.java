/* 文件职责：定义考试试卷题目的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamPaperQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义考试试卷题目的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamPaperQuestionMapper {
    @Select("""
            SELECT id, paper_id, question_id, sort_order, score,
                   question_type_snapshot, stem_snapshot, options_snapshot,
                   answer_snapshot, analysis_snapshot, created_at
            FROM exam_paper_question
            WHERE paper_id = #{paperId}
            ORDER BY sort_order ASC, id ASC
            """)
    /** 执行 findByPaperId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ExamPaperQuestion> findByPaperId(Long paperId);

    @Insert("""
            INSERT INTO exam_paper_question (
                paper_id, question_id, sort_order, score, question_type_snapshot,
                stem_snapshot, options_snapshot, answer_snapshot, analysis_snapshot
            ) VALUES (
                #{paperId}, #{questionId}, #{sortOrder}, #{score}, #{questionTypeSnapshot},
                #{stemSnapshot}, #{optionsSnapshot}, #{answerSnapshot}, #{analysisSnapshot}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(ExamPaperQuestion question);

    @Delete("DELETE FROM exam_paper_question WHERE paper_id = #{paperId}")
    /** 执行 deleteByPaperId 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteByPaperId(Long paperId);
}
