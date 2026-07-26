import assert from 'node:assert/strict'
import test from 'node:test'

import {
  findMarkdownContentReferences,
  replaceMarkdownContentReference,
} from '../src/utils/markdown.ts'

test('finds unique internal learning content references', () => {
  assert.deepEqual(
    findMarkdownContentReferences(
      '[资料一](content-reference://12) [重复](content-reference://12) [资料二](content-reference://20)',
    ),
    [12, 20],
  )
})

test('replaces an internal reference with a platform content route', () => {
  assert.equal(
    replaceMarkdownContentReference('[资料](content-reference://12)', 12),
    '[资料](/contents/12)',
  )
})
