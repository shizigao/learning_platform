import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

import { canChangeAdminUserStatus, shouldShowExamCountdown } from '../src/utils/ui-state.ts'

function viewSource(name: string): string {
  return readFileSync(new URL(`../src/views/${name}.vue`, import.meta.url), 'utf8')
}

test('submitted exams do not render or start the countdown', () => {
  const source = viewSource('ExamEntryView')
  assert.equal(shouldShowExamCountdown(1, 'STARTED', false), true)
  assert.equal(shouldShowExamCountdown(1, 'SUBMITTED', false), false)
  assert.equal(shouldShowExamCountdown(1, 'GRADING', false), false)
  assert.equal(shouldShowExamCountdown(1, 'COMPLETED', false), false)
  assert.equal(shouldShowExamCountdown(undefined, undefined, false), false)
  assert.equal(shouldShowExamCountdown(1, 'STARTED', true), false)
  assert.match(source, /shouldShowExamCountdown/)
  assert.match(source, /v-if="showCountdown"/)
  assert.match(source, /if \(showCountdown\.value && eligibility\.deadlineAt\)/)
})

test('home page contains no fabricated progress or obsolete availability notice', () => {
  const source = viewSource('HomeView')
  assert.doesNotMatch(source, /68%/)
  assert.doesNotMatch(source, /各业务入口将在后续阶段逐步开放/)
  assert.match(source, /核心功能已开放/)
})

test('publisher paper timestamps use the existing readable date formatter', () => {
  const source = viewSource('PublisherPapersView')
  assert.match(source, /function displayTime/)
  assert.match(source, /displayTime\(row\.updatedAt\)/)
  assert.doesNotMatch(source, /prop="updatedAt"/)
})

test('current administrator cannot click their own account status action', () => {
  const source = viewSource('AdminWorkspaceView')
  assert.equal(canChangeAdminUserStatus(1, 'ACTIVE', 1), false)
  assert.equal(canChangeAdminUserStatus(1, 'LOCKED', 2), false)
  assert.equal(canChangeAdminUserStatus(1, 'ACTIVE', 2), true)
  assert.equal(canChangeAdminUserStatus(1, 'DISABLED', 2), true)
  assert.match(source, /:disabled="!canChangeAdminUserStatus/)
})

test('commerce all-products filter uses a real radio value', () => {
  const source = viewSource('CommerceView')
  assert.doesNotMatch(source, /:value="undefined"/)
  assert.match(source, /el-radio-button value="ALL"/)
})

test('exam result summary exposes pending and partial-credit counts', () => {
  const source = viewSource('ExamResultView')
  assert.match(source, /待批改/)
  assert.match(source, /部分得分/)
  assert.match(source, /pendingReviewCount/)
  assert.match(source, /partialCreditCount/)
})
