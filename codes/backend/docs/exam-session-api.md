# 考生考试会话接口

## 接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/exams` | 查询当前用户被指定参加的考试 |
| GET | `/api/exams/{examId}/overview` | 查询考试说明、试卷摘要和当前资格，不返回题目 |
| GET | `/api/exams/{examId}/eligibility` | 重新检查指定关系、开放时间和作答状态 |
| POST | `/api/exams/{examId}/start` | 开始或幂等地继续同一次考试，返回安全试卷 |
| GET | `/api/exams/{examId}/session` | 恢复尚未提交的考试会话和已保存答案 |
| PUT | `/api/exams/{examId}/answers/{questionId}` | 保存或覆盖一道题的答案 |
| PUT | `/api/exams/{examId}/answers` | 批量保存多道题的答案 |
| POST | `/api/exams/{examId}/submit` | 手动交卷；重复请求返回同一次提交结果 |
| GET | `/api/exams/{examId}/result` | 查询当前考生可见的成绩与每题得分 |
| GET | `/api/publisher/exams/{examId}/grading/attempts` | 查询发布者考试的已交卷记录 |
| GET | `/api/publisher/exams/{examId}/grading/attempts/{attemptId}` | 查询阅卷详情和参考答案 |
| PUT | `/api/publisher/exams/{examId}/grading/attempts/{attemptId}/answers/{answerId}` | 批改一道主观题 |
| POST | `/api/publisher/exams/{examId}/grading/attempts/{attemptId}/complete` | 确认本次阅卷完成并生成最终成绩 |
| GET | `/api/publisher/exams/{examId}/grading/statistics` | 查询参与、成绩和每题正确率统计 |

## 服务端计时规则

开始接口在事务中锁定当前考生记录。首次调用创建唯一的 `exam_attempt`，记录服务端
`started_at`，并按以下规则计算 `deadline_at`：

```text
deadline_at = min(started_at + duration_minutes, exam.end_at)
```

同一考生重复调用开始接口不会创建第二条记录，也不会重置开始时间或延长截止时间。接口同时
返回 `serverTime`、`deadlineAt` 和 `remainingSeconds`；前端倒计时只做展示，不能改变服务端
保存的截止时间。

未指定用户返回 403；未开始、已结束、已提交、缺考或个人时间已耗尽时禁止开始。开始响应中的
题目不包含答案、解析和选项正确标记。

## 答案保存与恢复

开始考试时，服务端为试卷中的每道题创建唯一答题记录。单题保存和批量保存均采用覆盖更新，
同一场考试、同一道题始终只有一条记录，因此网络重试不会产生重复答案。刷新页面后调用会话
恢复接口，可取回已保存的选项值或文本答案。

服务端按题型校验答案格式：单选题和判断题只能保存一个值，多选题可保存多个值，填空题按
空位顺序保存，简答题保存文本。考试已提交或服务端截止时间已到后，所有保存请求都会被拒绝。

## 交卷与超时处理

手动交卷在数据库事务内锁定考试记录，将 `exam_attempt` 和 `exam_candidate` 同步更新为已提交。
重复交卷不会重复生成结果，也不会改变首次提交时间。

后台定时任务默认每 5 秒扫描一次 MySQL 中已到截止时间但仍在作答的记录，并以 `TIMEOUT` 类型
自动提交。MySQL 是截止时间和提交状态的可靠数据源；Redis 只缓存倒计时和最近保存时间，
Redis 不可用时答题保存、恢复和超时交卷仍可正常工作。扫描间隔可通过
`EXAM_TIMEOUT_SCAN_MS` 调整。

## 评分规则

- 单选、多选和判断题按正确答案快照精确匹配，答对得满分，否则得 0 分。
- 配置为自动评分的填空题逐空匹配可接受答案，可配置是否区分大小写；按答对空位比例计算得分。
- 未配置自动评分的填空题和已作答的简答题进入 `PENDING_REVIEW`。
- 未作答题目直接记 0 分并保留 `UNANSWERED`，不会进入人工阅卷队列。
- 组卷时同时固化正确答案、填空评分方式和大小写规则，后续修改题库不会改变已生成试卷的评分规则。

人工评分必须在 0 到本题满分之间，最多保留两位小数。每道待阅题评分后仍需调用“完成阅卷”
接口；只有不存在 `PENDING_REVIEW` 的题目时才能完成。作答记录状态依次为
`SUBMITTED -> GRADING -> COMPLETED`，没有待阅题时会在交卷事务中直接进入 `COMPLETED`。

## 成绩与答案可见性

交卷后生成唯一 `exam_result`，保存客观分、主观分、总分、是否及格、正确/错误/未答题数和
每题得分。`showResultImmediately=true` 时允许考生查看尚未完成主观题批改的暂定成绩；
关闭时需等待人工阅卷完成。无论该配置为何，阅卷完成后的最终成绩均可查看。

正确答案和解析还需同时满足以下条件才返回给考生：

1. 阅卷已经完成；
2. 考试配置 `showAnswerAfterFinish=true`；
3. 当前服务端时间不早于整场考试结束时间。

发布者阅卷接口始终可以读取答案快照和解析；非考试所有者访问会被拒绝。

## 统计口径

- `totalCandidates`：本场考试指定考生总数。
- `participatedCount`：已经创建作答记录的考生数。
- `submittedCount`：已经手动或超时交卷的考生数。
- `notParticipatedCount`：尚未创建作答记录的指定考生数。
- `gradedCount`：已经完成全部评分并生成最终成绩的考生数。
- 平均分、最高分、最低分、及格人数和及格率只统计 `gradingCompleted=true` 的最终成绩。
- 每题正确率以已完成评分人数为分母；未作答和未全对均不计入正确人数，待阅卷记录不参与计算。

前端对应入口：

- 考生：`/exams/{examId}` 在线答题，`/exams/{examId}/result` 查看成绩。
- 发布者：考试管理列表点击“阅卷/统计”，进入
  `/publisher/exams/{examId}/grading` 完成人工阅卷和统计查看。
