import assert from 'node:assert/strict'
import test from 'node:test'

import {
  examAnswerPayload,
  minimumAnswerValues,
} from '../src/utils/exam-answer.ts'

test('multiple choice starts with an empty array and removes placeholder values', () => {
  assert.equal(minimumAnswerValues('MULTIPLE_CHOICE', 0), 0)
  assert.deepEqual(
    examAnswerPayload(
      { questionType: 'MULTIPLE_CHOICE' },
      { values: ['', 'A', 'B'], text: '' },
    ),
    { values: ['A', 'B'], text: null },
  )
})

test('true or false keeps one model slot and submits FALSE', () => {
  assert.equal(minimumAnswerValues('TRUE_FALSE', 0), 1)
  assert.deepEqual(
    examAnswerPayload(
      { questionType: 'TRUE_FALSE' },
      { values: ['FALSE'], text: '' },
    ),
    { values: ['FALSE'], text: null },
  )
})

test('short answer only submits text', () => {
  assert.equal(minimumAnswerValues('SHORT_ANSWER', 0), 0)
  assert.deepEqual(
    examAnswerPayload(
      { questionType: 'SHORT_ANSWER' },
      { values: [''], text: '数据库事务具有 ACID 特性。' },
    ),
    { values: [], text: '数据库事务具有 ACID 特性。' },
  )
})

test('fill blank preserves positional empty values', () => {
  assert.equal(minimumAnswerValues('FILL_BLANK', 2), 2)
  assert.deepEqual(
    examAnswerPayload(
      { questionType: 'FILL_BLANK' },
      { values: ['Java', ''], text: '' },
    ),
    { values: ['Java', ''], text: null },
  )
})
