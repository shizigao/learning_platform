# 题目选项与答案存储规范

## 1. 统一答案结构

所有题型都通过 `question.answer_json` 保存以下 JSON 对象：

```json
{
  "acceptedAnswers": [
    ["答案值或可接受写法"]
  ]
}
```

- 外层数组表示按顺序排列的答案位置。
- 内层数组表示同一位置允许匹配的一个或多个写法。
- 所有答案在保存前去除首尾空白；选择题与判断题答案转为大写代码。

## 2. 五类题型示例

| 题型 | `question_option` | `answer_json` 示例 |
| --- | --- | --- |
| 单选 | 至少两个选项 | `{"acceptedAnswers":[["A"]]}` |
| 多选 | 至少两个选项 | `{"acceptedAnswers":[["A","C"]]}` |
| 判断 | 系统生成 `TRUE`、`FALSE` | `{"acceptedAnswers":[["TRUE"]]}` |
| 填空 | 无选项 | `{"acceptedAnswers":[["Spring","spring"],["Boot"]]}` |
| 简答 | 无选项 | `{"acceptedAnswers":[["参考答案正文"]]}` |

`question.answer_text` 仅同步保存简答题参考答案，供后续人工阅卷查询；权威结构仍是 `answer_json`。

## 3. 接口隔离

- 发布者管理响应使用 `QuestionManagementResponse`，包含答案和解析。
- 考生作答响应必须使用 `CandidateQuestionResponse`，该类型不声明答案、解析或选项正确性字段。
- `QuestionOptionResponse` 只包含选项 ID、代码、文本和顺序，不包含数据库中的 `is_correct`。
- 后续组卷快照及考生作答接口不得直接序列化 `Question` 或 `QuestionOption` 领域对象。

## 4. 当前管理接口

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET/POST | `/api/publisher/question-banks` | 查询、新建当前发布者的题库 |
| PUT/DELETE | `/api/publisher/question-banks/{bankId}` | 修改、删除空题库 |
| GET/POST | `/api/publisher/questions` | 分页筛选、新建题目 |
| GET/PUT/DELETE | `/api/publisher/questions/{questionId}` | 查询、修改、逻辑删除题目 |

上述接口仅允许 `PUBLISHER` 或 `ADMIN` 角色访问，并在业务层再次校验题库所有者。
