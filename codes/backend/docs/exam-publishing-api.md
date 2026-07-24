# 固定试卷与考试发布接口

## 固定试卷

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/publisher/papers` | 分页查询、新建试卷 |
| GET/PUT/DELETE | `/api/publisher/papers/{paperId}` | 预览、修改、逻辑删除试卷 |
| PUT | `/api/publisher/papers/{paperId}/questions` | 整体替换手工选题、顺序和分值 |

完成组卷后，服务端计算 `question_count` 与 `total_score`，并将题型、题干、选项、答案和解析保存到 `exam_paper_question` 快照。原题后续修改不会改变已生成试卷。试卷被考试引用后不可继续修改。

## 考试管理

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/publisher/exams` | 分页查询、新建考试 |
| GET/PUT/DELETE | `/api/publisher/exams/{examId}` | 查询、修改、删除草稿考试 |
| POST | `/api/publisher/exams/{examId}/publish` | 发布考试并扣减一次额度 |
| POST | `/api/publisher/exams/{examId}/cancel` | 取消尚未开始的已发布考试 |
| GET | `/api/publisher/exams/quota` | 查询当前考试发布额度 |
| GET | `/api/publisher/exam-candidates?keyword=` | 按用户名或昵称检索可指定的正常账号 |

考试创建和修改时校验：

- 试卷已完成组卷且属于当前发布者；
- 结束时间晚于开始时间和当前时间；
- 答题时长不超过考试开放时长；
- 及格分不超过试卷总分；
- 指定考生存在、状态正常且不重复；
- 只有草稿考试可以修改或删除。

发布操作在同一数据库事务中锁定考试、扣减 `EXAM_QUOTA` 并更新状态。对已经发布的同一考试重复调用发布接口会直接返回已有结果，不会重复扣减额度。取消考试不返还已消耗额度。

## 考生接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/exams` | 查询当前用户被指定参加的已发布考试 |
| GET | `/api/exams/{examId}` | 在开考后取得安全试卷 |
| GET | `/api/exams/{examId}/overview` | 查询考试说明与资格，不返回题目 |
| GET | `/api/exams/{examId}/eligibility` | 检查当前考生是否可以开始或继续 |
| POST | `/api/exams/{examId}/start` | 由服务端创建或恢复唯一作答记录并计算截止时间 |

考生必须位于 `exam_candidate` 中。考生试卷响应仅包含题干、选项、顺序和分值，不包含答案、解析或选项正确标记。
