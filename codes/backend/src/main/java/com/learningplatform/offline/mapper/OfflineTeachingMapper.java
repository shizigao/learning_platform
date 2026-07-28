/* 文件职责：定义线下教学教学的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.mapper;

import com.learningplatform.offline.domain.OfflineStudentPreference;
import com.learningplatform.offline.domain.OfflineTeacherApplication;
import com.learningplatform.offline.domain.OfflineTeacherProfile;
import com.learningplatform.offline.domain.OfflineTeacherRecommendation;
import com.learningplatform.offline.domain.TeacherApplicationAdminView;
import com.learningplatform.offline.domain.TeacherApplicationStatus;
import com.learningplatform.offline.domain.TeacherProfileStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义线下教学教学的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface OfflineTeachingMapper {
    /** 定义 APPLICATION_COLUMNS 常量，统一该组件使用的固定规则或默认值。 */
    String APPLICATION_COLUMNS = """
            id, user_id, teacher_name, id_card_ciphertext, id_card_iv,
            id_card_hmac, id_card_masked, gender, education_level,
            education_background, institution, province, city, district,
            bio, teaching_content, teaching_tags, hourly_rate,
            availability, price_description, contact_wechat, contact_qq, contact_email,
            status, rejection_reason, submitted_at, reviewed_at, reviewed_by,
            version, created_at, updated_at
            """;

    /** 定义 PROFILE_COLUMNS 常量，统一该组件使用的固定规则或默认值。 */
    String PROFILE_COLUMNS = """
            p.id, p.user_id, p.source_application_id, p.teacher_name, p.gender,
            p.education_level, p.education_background, p.institution,
            p.province, p.city, p.district, p.bio, p.teaching_content,
            p.teaching_tags, p.availability, p.hourly_rate, p.price_description,
            p.contact_wechat, p.contact_qq, p.contact_email, p.status,
            p.suspended_reason, p.approved_at, p.approved_by,
            p.created_at, p.updated_at, u.username, u.nickname
            """;

    @Select("SELECT " + APPLICATION_COLUMNS
            + " FROM offline_teacher_application WHERE user_id = #{userId} AND deleted = 0")
    /** 执行 findApplicationByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<OfflineTeacherApplication> findApplicationByUserId(Long userId);

    @Select("SELECT " + APPLICATION_COLUMNS
            + " FROM offline_teacher_application WHERE id = #{id} AND deleted = 0")
    /** 执行 findApplicationById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<OfflineTeacherApplication> findApplicationById(Long id);

    @Insert("""
            INSERT INTO offline_teacher_application (
              user_id, teacher_name, id_card_ciphertext, id_card_iv,
              id_card_hmac, id_card_masked, gender, education_level,
              education_background, institution, province, city, district,
              bio, teaching_content, teaching_tags, availability, hourly_rate,
              price_description, contact_wechat, contact_qq, contact_email,
              status, version
            ) VALUES (
              #{userId}, #{teacherName}, #{idCardCiphertext}, #{idCardIv},
              #{idCardHmac}, #{idCardMasked}, #{gender}, #{educationLevel},
              #{educationBackground}, #{institution}, #{province}, #{city}, #{district},
              #{bio}, #{teachingContent}, #{teachingTags}, #{availability}, #{hourlyRate},
              #{priceDescription}, #{contactWechat}, #{contactQq}, #{contactEmail},
              'DRAFT', 0
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertApplication(OfflineTeacherApplication application);

    @Update("""
            UPDATE offline_teacher_application
            SET teacher_name = #{teacherName},
              id_card_ciphertext = #{idCardCiphertext},
              id_card_iv = #{idCardIv},
              id_card_hmac = #{idCardHmac},
              id_card_masked = #{idCardMasked},
              gender = #{gender},
              education_level = #{educationLevel},
              education_background = #{educationBackground},
              institution = #{institution},
              province = #{province},
              city = #{city},
              district = #{district},
              bio = #{bio},
              teaching_content = #{teachingContent},
              teaching_tags = #{teachingTags},
              availability = #{availability},
              hourly_rate = #{hourlyRate},
              price_description = #{priceDescription},
              contact_wechat = #{contactWechat},
              contact_qq = #{contactQq},
              contact_email = #{contactEmail},
              status = 'DRAFT',
              rejection_reason = NULL,
              submitted_at = NULL,
              reviewed_at = NULL,
              reviewed_by = NULL,
              version = version + 1,
              deleted = 0
            WHERE id = #{id} AND user_id = #{userId}
              AND status <> 'PENDING' AND deleted = 0
            """)
    /** 执行 updateApplication 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateApplication(OfflineTeacherApplication application);

    @Update("""
            UPDATE offline_teacher_application
            SET status = 'PENDING', submitted_at = #{submittedAt},
                rejection_reason = NULL, reviewed_at = NULL, reviewed_by = NULL,
                version = version + 1
            WHERE id = #{id} AND user_id = #{userId} AND status = 'DRAFT'
              AND deleted = 0
            """)
    /** 执行 submitApplication 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int submitApplication(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("submittedAt") LocalDateTime submittedAt
    );

    @Update("""
            UPDATE offline_teacher_application
            SET status = #{status}, rejection_reason = #{reason},
                reviewed_at = #{reviewedAt}, reviewed_by = #{reviewedBy},
                version = version + 1
            WHERE id = #{id} AND status = 'PENDING' AND deleted = 0
            """)
    /** 执行 reviewApplication 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int reviewApplication(
            @Param("id") Long id,
            @Param("status") TeacherApplicationStatus status,
            @Param("reason") String reason,
            @Param("reviewedAt") LocalDateTime reviewedAt,
            @Param("reviewedBy") Long reviewedBy
    );

    @Insert("""
            INSERT INTO offline_teacher_profile (
              user_id, source_application_id, teacher_name, gender,
              education_level, education_background, institution,
              province, city, district, bio, teaching_content, teaching_tags,
              availability, hourly_rate, price_description, contact_wechat, contact_qq,
              contact_email, status, approved_at, approved_by, version
            )
            SELECT user_id, id, teacher_name, gender, education_level,
              education_background, institution, province, city, district,
              bio, teaching_content, teaching_tags, availability, hourly_rate,
              price_description, contact_wechat, contact_qq, contact_email,
              'ACTIVE', #{approvedAt}, #{approvedBy}, 0
            FROM offline_teacher_application
            WHERE id = #{applicationId} AND status = 'APPROVED' AND deleted = 0
            ON DUPLICATE KEY UPDATE
              source_application_id = VALUES(source_application_id),
              teacher_name = VALUES(teacher_name),
              gender = VALUES(gender),
              education_level = VALUES(education_level),
              education_background = VALUES(education_background),
              institution = VALUES(institution),
              province = VALUES(province),
              city = VALUES(city),
              district = VALUES(district),
              bio = VALUES(bio),
              teaching_content = VALUES(teaching_content),
              teaching_tags = VALUES(teaching_tags),
              availability = VALUES(availability),
              hourly_rate = VALUES(hourly_rate),
              price_description = VALUES(price_description),
              contact_wechat = VALUES(contact_wechat),
              contact_qq = VALUES(contact_qq),
              contact_email = VALUES(contact_email),
              status = 'ACTIVE',
              suspended_reason = NULL,
              approved_at = VALUES(approved_at),
              approved_by = VALUES(approved_by),
              version = offline_teacher_profile.version + 1,
              deleted = 0
            """)
    /** 执行 upsertProfileFromApplication 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int upsertProfileFromApplication(
            @Param("applicationId") Long applicationId,
            @Param("approvedAt") LocalDateTime approvedAt,
            @Param("approvedBy") Long approvedBy
    );

    @Select("""
            SELECT
            """ + PROFILE_COLUMNS + """
            FROM offline_teacher_profile p
            INNER JOIN `user` u ON u.id = p.user_id
            WHERE p.id = #{id} AND p.deleted = 0 AND u.deleted = 0
            """)
    /** 执行 findProfileById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<OfflineTeacherProfile> findProfileById(Long id);

    @Select("""
            SELECT
            """ + PROFILE_COLUMNS + """
            FROM offline_teacher_profile p
            INNER JOIN `user` u ON u.id = p.user_id
            WHERE p.user_id = #{userId} AND p.deleted = 0 AND u.deleted = 0
            """)
    /** 执行 findProfileByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<OfflineTeacherProfile> findProfileByUserId(Long userId);

    @Select("""
            <script>
            SELECT
            """ + PROFILE_COLUMNS + """
            FROM offline_teacher_profile p
            INNER JOIN `user` u ON u.id = p.user_id
            WHERE p.status = 'ACTIVE' AND p.deleted = 0
              AND u.status = 'ACTIVE' AND u.deleted = 0
            <if test='keyword != null'>
              AND (p.teacher_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR p.institution LIKE CONCAT('%', #{keyword}, '%')
                OR p.teaching_content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='province != null'>AND p.province = #{province}</if>
            <if test='city != null'>AND p.city = #{city}</if>
            <if test='teachingTag != null'>
              AND CAST(p.teaching_tags AS CHAR) LIKE CONCAT('%', #{teachingTag}, '%')
            </if>
            <if test='maxHourlyRate != null'>AND p.hourly_rate &lt;= #{maxHourlyRate}</if>
            ORDER BY p.updated_at DESC, p.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findProfiles 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<OfflineTeacherProfile> findProfiles(
            @Param("keyword") String keyword,
            @Param("province") String province,
            @Param("city") String city,
            @Param("teachingTag") String teachingTag,
            @Param("maxHourlyRate") BigDecimal maxHourlyRate,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM offline_teacher_profile p
            INNER JOIN `user` u ON u.id = p.user_id
            WHERE p.status = 'ACTIVE' AND p.deleted = 0
              AND u.status = 'ACTIVE' AND u.deleted = 0
            <if test='keyword != null'>
              AND (p.teacher_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR p.institution LIKE CONCAT('%', #{keyword}, '%')
                OR p.teaching_content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='province != null'>AND p.province = #{province}</if>
            <if test='city != null'>AND p.city = #{city}</if>
            <if test='teachingTag != null'>
              AND CAST(p.teaching_tags AS CHAR) LIKE CONCAT('%', #{teachingTag}, '%')
            </if>
            <if test='maxHourlyRate != null'>AND p.hourly_rate &lt;= #{maxHourlyRate}</if>
            </script>
            """)
    /** 执行 countProfiles 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countProfiles(
            @Param("keyword") String keyword,
            @Param("province") String province,
            @Param("city") String city,
            @Param("teachingTag") String teachingTag,
            @Param("maxHourlyRate") BigDecimal maxHourlyRate
    );

    @Select("""
            SELECT
            """ + PROFILE_COLUMNS + """
            FROM offline_teacher_profile p
            INNER JOIN `user` u ON u.id = p.user_id
            WHERE p.status = 'ACTIVE' AND p.deleted = 0
              AND u.status = 'ACTIVE' AND u.deleted = 0
            ORDER BY
              CASE WHEN p.city = #{city} THEN 0
                   WHEN p.province = #{province} THEN 1 ELSE 2 END,
              CASE WHEN p.hourly_rate <= #{maxRate} THEN 0 ELSE 1 END,
              p.updated_at DESC
            LIMIT 100
            """)
    /** 执行 findRecommendationCandidates 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<OfflineTeacherProfile> findRecommendationCandidates(
            @Param("province") String province,
            @Param("city") String city,
            @Param("maxRate") BigDecimal maxRate
    );

    @Update("""
            UPDATE offline_teacher_profile
            SET status = #{status}, suspended_reason = #{reason}, version = version + 1
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 updateProfileStatus 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateProfileStatus(
            @Param("id") Long id,
            @Param("status") TeacherProfileStatus status,
            @Param("reason") String reason
    );

    @Select("""
            <script>
            SELECT a.id, a.user_id, a.teacher_name, a.id_card_ciphertext,
              a.id_card_iv, a.id_card_hmac, a.id_card_masked, a.gender,
              a.education_level, a.education_background, a.institution,
              a.province, a.city, a.district, a.bio, a.teaching_content,
              a.teaching_tags, a.availability, a.hourly_rate, a.price_description,
              a.contact_wechat, a.contact_qq, a.contact_email, a.status,
              a.rejection_reason, a.submitted_at, a.reviewed_at, a.reviewed_by,
              a.version, a.created_at, a.updated_at, u.username, u.nickname
            FROM offline_teacher_application a
            INNER JOIN `user` u ON u.id = a.user_id
            WHERE a.deleted = 0 AND u.deleted = 0
            <if test='status != null'>AND a.status = #{status}</if>
            <if test='keyword != null'>
              AND (a.teacher_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR a.institution LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY CASE a.status WHEN 'PENDING' THEN 0 ELSE 1 END,
              a.submitted_at DESC, a.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findApplicationsForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<TeacherApplicationAdminView> findApplicationsForAdmin(
            @Param("status") TeacherApplicationStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM offline_teacher_application a
            INNER JOIN `user` u ON u.id = a.user_id
            WHERE a.deleted = 0 AND u.deleted = 0
            <if test='status != null'>AND a.status = #{status}</if>
            <if test='keyword != null'>
              AND (a.teacher_name LIKE CONCAT('%', #{keyword}, '%')
                OR u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                OR a.institution LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    /** 执行 countApplicationsForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countApplicationsForAdmin(
            @Param("status") TeacherApplicationStatus status,
            @Param("keyword") String keyword
    );

    @Insert("""
            INSERT INTO offline_student_preference (
              user_id, subject, current_level, learning_goals, weaknesses,
              province, city, district, max_hourly_rate, availability,
              teacher_preferences, additional_notes
            ) VALUES (
              #{userId}, #{subject}, #{currentLevel}, #{learningGoals}, #{weaknesses},
              #{province}, #{city}, #{district}, #{maxHourlyRate}, #{availability},
              #{teacherPreferences}, #{additionalNotes}
            )
            ON DUPLICATE KEY UPDATE
              subject = VALUES(subject), current_level = VALUES(current_level),
              learning_goals = VALUES(learning_goals), weaknesses = VALUES(weaknesses),
              province = VALUES(province), city = VALUES(city),
              district = VALUES(district), max_hourly_rate = VALUES(max_hourly_rate),
              availability = VALUES(availability),
              teacher_preferences = VALUES(teacher_preferences),
              additional_notes = VALUES(additional_notes)
            """)
    /** 执行 upsertPreference 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int upsertPreference(OfflineStudentPreference preference);

    @Select("""
            SELECT user_id, subject, current_level, learning_goals, weaknesses,
              province, city, district, max_hourly_rate, availability,
              teacher_preferences, additional_notes, created_at, updated_at
            FROM offline_student_preference WHERE user_id = #{userId}
            """)
    /** 执行 findPreference 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<OfflineStudentPreference> findPreference(Long userId);

    @Insert("""
            INSERT INTO offline_teacher_recommendation (
              task_id, user_id, preference_snapshot, candidate_snapshot,
              recommendation_json, input_snapshot_hash
            ) VALUES (
              #{taskId}, #{userId}, #{preferenceSnapshot}, #{candidateSnapshot},
              #{recommendationJson}, #{inputSnapshotHash}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertRecommendation(OfflineTeacherRecommendation recommendation);

    @Select("""
            SELECT id, task_id, user_id, preference_snapshot, candidate_snapshot,
              recommendation_json, input_snapshot_hash, created_at
            FROM offline_teacher_recommendation
            WHERE task_id = #{taskId}
            """)
    /** 执行 findRecommendationByTaskId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<OfflineTeacherRecommendation> findRecommendationByTaskId(Long taskId);
}
