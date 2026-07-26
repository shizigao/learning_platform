-- 线下教师推荐功能测试数据：20 个发布者账号与 20 份已审核教师资料。
-- 仅用于本地开发数据库，可重复执行；不会删除其他业务数据。
-- 测试账号：teacher_test_01 至 teacher_test_20
-- 统一密码：TeacherTest@2026
-- 前置条件：已执行 008_offline_teacher_availability.sql，且系统中至少有一个管理员。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

START TRANSACTION;

SET @seed_password_hash =
    '$2a$10$9RF44zFrcFYod7TRrwsiFef9E3GBQyQB1tR8j8i71fLkxhkNnNiMC';
SET @seed_admin_id = (
    SELECT MIN(u.id)
    FROM `user` u
    INNER JOIN `user_role` ur ON ur.user_id = u.id
    INNER JOIN `role` r ON r.id = ur.role_id
    WHERE r.code = 'ADMIN' AND r.enabled = 1
      AND u.status = 'ACTIVE' AND u.deleted = 0
);

DROP TEMPORARY TABLE IF EXISTS `tmp_offline_teacher_seed`;
CREATE TEMPORARY TABLE `tmp_offline_teacher_seed` (
    `username` VARCHAR(64) NOT NULL PRIMARY KEY,
    `nickname` VARCHAR(64) NOT NULL,
    `teacher_name` VARCHAR(64) NOT NULL,
    `gender` VARCHAR(16) NOT NULL,
    `education_level` VARCHAR(32) NOT NULL,
    `education_background` VARCHAR(1000) NOT NULL,
    `institution` VARCHAR(200) NULL,
    `province` VARCHAR(100) NOT NULL,
    `city` VARCHAR(100) NOT NULL,
    `district` VARCHAR(100) NULL,
    `bio` VARCHAR(2000) NOT NULL,
    `teaching_content` VARCHAR(2000) NOT NULL,
    `teaching_tags` JSON NOT NULL,
    `availability` VARCHAR(1000) NOT NULL,
    `hourly_rate` DECIMAL(10,2) NOT NULL,
    `price_description` VARCHAR(500) NULL,
    `contact_wechat` VARCHAR(100) NOT NULL
);

INSERT INTO `tmp_offline_teacher_seed` VALUES
('teacher_test_01', '陈老师·高中数学', '陈明远', 'MALE', 'MASTER',
 '华南师范大学数学教育硕士，8年高中数学教学经验',
 '广州启航教育', '广东省', '广州市', '天河区',
 '擅长诊断知识漏洞，重视解题思路和规范书写。',
 '高中数学、高考数学、函数、解析几何、概率统计',
 JSON_ARRAY('高中数学', '高考数学', '函数', '解析几何'),
 '工作日19:00-21:30，周六09:00-18:00', 80.00,
 '两小时起约，可提供阶段学习计划', 'teacher_test_01'),
('teacher_test_02', '周老师·英语口语', '周雅雯', 'FEMALE', 'MASTER',
 '英语语言文学硕士，持有英语专业八级证书',
 '广州博雅语言中心', '广东省', '广州市', '越秀区',
 '注重情景交流和发音纠正，适合希望提升表达自信的学生。',
 '英语口语、日常会话、商务英语、发音纠正',
 JSON_ARRAY('英语口语', '商务英语', '发音'),
 '周二、周四19:00-21:00，周日全天', 100.00,
 '可线上预沟通学习目标', 'teacher_test_02'),
('teacher_test_03', '林老师·Java开发', '林志恒', 'MALE', 'BACHELOR',
 '计算机科学本科，10年Java后端开发与企业培训经验',
 '深圳代码工坊', '广东省', '深圳市', '南山区',
 '以项目驱动教学，帮助学习者建立工程化开发能力。',
 'Java基础、Spring Boot、MySQL、后端项目实战',
 JSON_ARRAY('Java', 'Spring Boot', '后端开发', 'MySQL'),
 '工作日20:00-22:00，周六14:00-20:00', 120.00,
 '项目辅导按学习阶段协商', 'teacher_test_03'),
('teacher_test_04', '王老师·数据库', '王思齐', 'FEMALE', 'MASTER',
 '软件工程硕士，数据库系统与数据建模方向',
 '广州智数教育', '广东省', '广州市', '海珠区',
 '善于通过案例解释抽象概念，提供SQL练习与复盘。',
 '数据库原理、MySQL、SQL优化、事务与索引',
 JSON_ARRAY('数据库', 'MySQL', 'SQL', '事务'),
 '周三19:00-21:00，周六、周日09:00-17:00', 90.00,
 '首次课程可进行基础水平评估', 'teacher_test_04'),
('teacher_test_05', '赵老师·高中物理', '赵启航', 'MALE', 'BACHELOR',
 '物理学本科，6年高中物理辅导经验',
 '佛山明理教育', '广东省', '佛山市', '禅城区',
 '强调物理模型、受力分析和实验思维。',
 '高中物理、力学、电磁学、高考物理',
 JSON_ARRAY('高中物理', '力学', '电磁学', '高考'),
 '周六、周日14:00-20:00', 75.00,
 '可按章节安排专题课', 'teacher_test_05'),
('teacher_test_06', '刘老师·高中化学', '刘若琳', 'FEMALE', 'MASTER',
 '化学教育硕士，熟悉高中化学新课标',
 '广州格致课堂', '广东省', '广州市', '番禺区',
 '注重实验现象、反应规律和知识网络构建。',
 '高中化学、有机化学、化学实验、高考化学',
 JSON_ARRAY('高中化学', '有机化学', '化学实验'),
 '工作日18:30-21:00，周日09:00-12:00', 85.00,
 '支持错题专题整理', 'teacher_test_06'),
('teacher_test_07', '孙老师·语文阅读写作', '孙文清', 'FEMALE', 'MASTER',
 '汉语言文学硕士，长期从事中学语文教学',
 '广州青禾语文', '广东省', '广州市', '白云区',
 '关注阅读方法、表达结构与写作素材积累。',
 '初高中语文、阅读理解、作文提升、古诗文',
 JSON_ARRAY('语文', '阅读理解', '作文', '古诗文'),
 '周六09:00-12:00，周日14:00-18:00', 70.00,
 '作文可提供课后批改', 'teacher_test_07'),
('teacher_test_08', '高老师·日语', '高桥然', 'MALE', 'BACHELOR',
 '日语专业本科，日语能力考试N1',
 '深圳和风语言社', '广东省', '深圳市', '福田区',
 '从五十音到能力考试，重视听说读写综合训练。',
 '日语入门、JLPT N2/N1、日语会话',
 JSON_ARRAY('日语', 'JLPT', '日语会话'),
 '工作日19:30-22:00，周日下午', 95.00,
 '教材可根据目标共同确定', 'teacher_test_08'),
('teacher_test_09', '何老师·雅思', '何嘉怡', 'FEMALE', 'MASTER',
 '应用语言学硕士，雅思总分8分',
 '广州环球语言工作室', '广东省', '广州市', '天河区',
 '针对目标分数制定听说读写训练方案。',
 '雅思听力、雅思口语、雅思阅读、雅思写作',
 JSON_ARRAY('雅思', '英语', '留学考试'),
 '周五19:00-21:30，周末10:00-20:00', 180.00,
 '可按单项或全科辅导', 'teacher_test_09'),
('teacher_test_10', '郑老师·Python', '郑云峰', 'MALE', 'BACHELOR',
 '计算机本科，数据处理与自动化开发经验丰富',
 '杭州极客课堂', '浙江省', '杭州市', '西湖区',
 '适合零基础学习者，课堂包含大量可运行代码。',
 'Python基础、自动化办公、网络爬虫、数据分析',
 JSON_ARRAY('Python', '数据分析', '自动化'),
 '周一至周五19:00-22:00', 110.00,
 '可提供项目代码评审', 'teacher_test_10'),
('teacher_test_11', '沈老师·数据科学', '沈知行', 'MALE', 'DOCTOR',
 '统计学博士，研究方向为数据挖掘与统计建模',
 '上海数理研习社', '上海市', '上海市', '浦东新区',
 '强调数学原理与实践结合，适合本科及研究生。',
 '统计学、数据科学、R语言、机器学习基础',
 JSON_ARRAY('数据科学', '统计学', 'R语言', '机器学习'),
 '周六、周日09:00-18:00', 220.00,
 '研究生课题辅导需提前沟通', 'teacher_test_11'),
('teacher_test_12', '谢老师·前端开发', '谢安然', 'FEMALE', 'BACHELOR',
 '软件工程本科，7年前端开发与带教经验',
 '广州前端实验室', '广东省', '广州市', '黄埔区',
 '通过完整页面和小型项目讲解现代前端开发。',
 'HTML、CSS、JavaScript、Vue、TypeScript',
 JSON_ARRAY('前端开发', 'JavaScript', 'Vue', 'TypeScript'),
 '周一、周三、周五19:30-22:00', 115.00,
 '适合零基础或项目提升', 'teacher_test_12'),
('teacher_test_13', '彭老师·会计', '彭晓敏', 'FEMALE', 'MASTER',
 '会计学硕士，注册会计师，企业财务培训经验',
 '广州财经学习中心', '广东省', '广州市', '荔湾区',
 '结合业务案例讲解会计逻辑与考试重点。',
 '基础会计、财务管理、初级会计职称、CPA基础',
 JSON_ARRAY('会计', '财务管理', '职称考试'),
 '周六09:00-17:00，周日14:00-20:00', 130.00,
 '考证辅导可按科目安排', 'teacher_test_13'),
('teacher_test_14', '许老师·公考行测', '许正阳', 'MALE', 'MASTER',
 '公共管理硕士，5年公考笔试教学经验',
 '北京成公课堂', '北京市', '北京市', '海淀区',
 '重视时间管理、题型方法和阶段测评。',
 '公务员考试、行政职业能力测验、申论基础',
 JSON_ARRAY('公务员考试', '行测', '申论'),
 '工作日18:30-21:30，周末全天', 160.00,
 '提供阶段模考分析', 'teacher_test_14'),
('teacher_test_15', '唐老师·钢琴', '唐悦', 'FEMALE', 'MASTER',
 '音乐教育硕士，中央音乐学院钢琴考级指导经验',
 '广州乐章艺术中心', '广东省', '广州市', '海珠区',
 '耐心细致，兼顾基本功、乐感和练习习惯。',
 '少儿钢琴、成人钢琴、钢琴考级、乐理',
 JSON_ARRAY('钢琴', '音乐', '考级', '乐理'),
 '周三18:00-21:00，周六、周日全天', 200.00,
 '需学生自备钢琴或预约琴房', 'teacher_test_15'),
('teacher_test_16', '邓老师·美术', '邓可心', 'FEMALE', 'BACHELOR',
 '美术学本科，擅长素描、色彩与少儿创意美术',
 '深圳绘梦艺术空间', '广东省', '深圳市', '宝安区',
 '尊重个体表达，在系统训练中培养观察能力。',
 '素描、色彩、少儿美术、美术基础',
 JSON_ARRAY('美术', '素描', '色彩', '少儿美术'),
 '周六、周日09:00-19:00', 140.00,
 '材料费用另行协商', 'teacher_test_16'),
('teacher_test_17', '梁老师·小学数学', '梁美琪', 'FEMALE', 'BACHELOR',
 '小学教育本科，具有小学数学教师资格证',
 '广州童学成长中心', '广东省', '广州市', '番禺区',
 '通过图示、操作和生活情境帮助孩子理解数学。',
 '小学数学、思维训练、作业辅导、计算能力',
 JSON_ARRAY('小学数学', '思维训练', '作业辅导'),
 '工作日16:30-20:30，周六上午', 60.00,
 '适合小学一至六年级', 'teacher_test_17'),
('teacher_test_18', '罗老师·考研数学', '罗致远', 'MALE', 'DOCTOR',
 '应用数学博士，长期从事大学数学课程教学',
 '武汉研途数学', '湖北省', '武汉市', '洪山区',
 '注重概念体系、典型题型与复习节奏。',
 '高等数学、线性代数、概率论、考研数学',
 JSON_ARRAY('考研数学', '高等数学', '线性代数', '概率论'),
 '工作日19:00-22:00，周日全天', 150.00,
 '可制定长期备考计划', 'teacher_test_18'),
('teacher_test_19', '曹老师·机器学习', '曹睿', 'MALE', 'MASTER',
 '人工智能硕士，算法工程师与技术讲师',
 '广州智能技术社', '广东省', '广州市', '天河区',
 '从算法直觉到代码实现，帮助学习者完成作品。',
 '机器学习、深度学习、PyTorch、算法项目',
 JSON_ARRAY('机器学习', '人工智能', 'PyTorch', 'Python'),
 '周六14:00-22:00，周日09:00-18:00', 190.00,
 '需具备基础Python能力', 'teacher_test_19'),
('teacher_test_20', '冯老师·Excel办公', '冯静', 'FEMALE', 'ASSOCIATE',
 '信息管理专业，企业办公软件培训师',
 '广州职场技能工作室', '广东省', '广州市', '白云区',
 '面向实际工作场景，提供表格模板与练习案例。',
 'Excel函数、数据透视表、办公自动化、PPT基础',
 JSON_ARRAY('Excel', '办公自动化', '数据透视表'),
 '工作日14:00-18:00，周二、周四19:00-21:00', 65.00,
 '可按企业或个人场景定制', 'teacher_test_20');

INSERT INTO `user` (
    `username`, `password_hash`, `nickname`, `bio`, `status`, `deleted`
)
SELECT
    s.username,
    @seed_password_hash,
    s.nickname,
    CONCAT('线下教学推荐测试账号：', s.teaching_content),
    'ACTIVE',
    0
FROM `tmp_offline_teacher_seed` s
ON DUPLICATE KEY UPDATE
    `password_hash` = VALUES(`password_hash`),
    `nickname` = VALUES(`nickname`),
    `bio` = VALUES(`bio`),
    `status` = 'ACTIVE',
    `deleted` = 0;

INSERT IGNORE INTO `user_role` (`user_id`, `role_id`, `granted_by`)
SELECT u.id, r.id, @seed_admin_id
FROM `tmp_offline_teacher_seed` s
INNER JOIN `user` u ON u.username = s.username
INNER JOIN `role` r ON r.code IN ('USER', 'PUBLISHER') AND r.enabled = 1;

INSERT INTO `offline_teacher_application` (
    `user_id`, `teacher_name`, `id_card_ciphertext`, `id_card_iv`,
    `id_card_hmac`, `id_card_masked`, `gender`, `education_level`,
    `education_background`, `institution`, `province`, `city`, `district`,
    `bio`, `teaching_content`, `teaching_tags`, `availability`,
    `hourly_rate`, `price_description`, `contact_wechat`,
    `contact_qq`, `contact_email`, `status`, `submitted_at`,
    `reviewed_at`, `reviewed_by`, `deleted`
)
SELECT
    u.id,
    s.teacher_name,
    UNHEX(SHA2(CONCAT(s.username, ':ciphertext'), 512)),
    UNHEX(SUBSTRING(SHA2(CONCAT(s.username, ':iv'), 256), 1, 24)),
    UNHEX(SHA2(CONCAT(s.username, ':hmac'), 256)),
    CONCAT('TEST********', RIGHT(s.username, 2)),
    s.gender,
    s.education_level,
    s.education_background,
    s.institution,
    s.province,
    s.city,
    s.district,
    s.bio,
    s.teaching_content,
    s.teaching_tags,
    s.availability,
    s.hourly_rate,
    s.price_description,
    s.contact_wechat,
    NULL,
    CONCAT(s.username, '@example.test'),
    'APPROVED',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3),
    @seed_admin_id,
    1
FROM `tmp_offline_teacher_seed` s
INNER JOIN `user` u ON u.username = s.username
ON DUPLICATE KEY UPDATE
    `teacher_name` = VALUES(`teacher_name`),
    `gender` = VALUES(`gender`),
    `education_level` = VALUES(`education_level`),
    `education_background` = VALUES(`education_background`),
    `institution` = VALUES(`institution`),
    `province` = VALUES(`province`),
    `city` = VALUES(`city`),
    `district` = VALUES(`district`),
    `bio` = VALUES(`bio`),
    `teaching_content` = VALUES(`teaching_content`),
    `teaching_tags` = VALUES(`teaching_tags`),
    `availability` = VALUES(`availability`),
    `hourly_rate` = VALUES(`hourly_rate`),
    `price_description` = VALUES(`price_description`),
    `contact_wechat` = VALUES(`contact_wechat`),
    `contact_email` = VALUES(`contact_email`),
    `status` = 'APPROVED',
    `reviewed_at` = CURRENT_TIMESTAMP(3),
    `reviewed_by` = @seed_admin_id,
    `deleted` = 1;

INSERT INTO `offline_teacher_profile` (
    `user_id`, `source_application_id`, `teacher_name`, `gender`,
    `education_level`, `education_background`, `institution`,
    `province`, `city`, `district`, `bio`, `teaching_content`,
    `teaching_tags`, `availability`, `hourly_rate`, `price_description`,
    `contact_wechat`, `contact_qq`, `contact_email`, `status`,
    `approved_at`, `approved_by`, `deleted`
)
SELECT
    u.id,
    a.id,
    s.teacher_name,
    s.gender,
    s.education_level,
    s.education_background,
    s.institution,
    s.province,
    s.city,
    s.district,
    s.bio,
    s.teaching_content,
    s.teaching_tags,
    s.availability,
    s.hourly_rate,
    s.price_description,
    s.contact_wechat,
    NULL,
    CONCAT(s.username, '@example.test'),
    'ACTIVE',
    CURRENT_TIMESTAMP(3),
    @seed_admin_id,
    0
FROM `tmp_offline_teacher_seed` s
INNER JOIN `user` u ON u.username = s.username
INNER JOIN `offline_teacher_application` a ON a.user_id = u.id
ON DUPLICATE KEY UPDATE
    `source_application_id` = VALUES(`source_application_id`),
    `teacher_name` = VALUES(`teacher_name`),
    `gender` = VALUES(`gender`),
    `education_level` = VALUES(`education_level`),
    `education_background` = VALUES(`education_background`),
    `institution` = VALUES(`institution`),
    `province` = VALUES(`province`),
    `city` = VALUES(`city`),
    `district` = VALUES(`district`),
    `bio` = VALUES(`bio`),
    `teaching_content` = VALUES(`teaching_content`),
    `teaching_tags` = VALUES(`teaching_tags`),
    `availability` = VALUES(`availability`),
    `hourly_rate` = VALUES(`hourly_rate`),
    `price_description` = VALUES(`price_description`),
    `contact_wechat` = VALUES(`contact_wechat`),
    `contact_email` = VALUES(`contact_email`),
    `status` = 'ACTIVE',
    `suspended_reason` = NULL,
    `approved_at` = CURRENT_TIMESTAMP(3),
    `approved_by` = @seed_admin_id,
    `deleted` = 0;

DROP TEMPORARY TABLE `tmp_offline_teacher_seed`;

COMMIT;

SELECT COUNT(*) AS `seed_user_count`
FROM `user`
WHERE `username` LIKE 'teacher_test\\_%' AND `deleted` = 0;

SELECT COUNT(*) AS `seed_teacher_count`
FROM `offline_teacher_profile` p
INNER JOIN `user` u ON u.id = p.user_id
WHERE u.username LIKE 'teacher_test\\_%'
  AND p.status = 'ACTIVE' AND p.deleted = 0;
