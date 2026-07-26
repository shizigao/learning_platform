import assert from 'node:assert/strict'
import test from 'node:test'

import { calculateReadingProgress } from '../src/utils/reading-progress.ts'

test('calculates the beginning, middle and end of a long article', () => {
  assert.equal(calculateReadingProgress(100, 2000, 1000), 0)
  assert.equal(calculateReadingProgress(-450, 2000, 1000), 50)
  assert.equal(calculateReadingProgress(-1000, 2000, 1000), 100)
})

test('clamps reading progress and completes short visible content', () => {
  assert.equal(calculateReadingProgress(500, 2000, 1000), 0)
  assert.equal(calculateReadingProgress(-2000, 2000, 1000), 100)
  assert.equal(calculateReadingProgress(100, 500, 1000), 100)
})
