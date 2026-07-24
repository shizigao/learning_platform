import assert from 'node:assert/strict'
import test from 'node:test'

import { UPLOAD_RULES, validateUploadFile } from '../src/utils/upload-rules.ts'

function file(name: string, size: number): File {
  return { name, size } as File
}

test('exposes type and size guidance for every upload role', () => {
  for (const rule of Object.values(UPLOAD_RULES)) {
    assert.ok(rule.extensions.length > 0)
    assert.ok(rule.maxBytes > 0)
    assert.match(rule.description, /支持/)
    assert.match(rule.description, /不能超过/)
  }
})

test('rejects a file larger than the selected role limit', () => {
  const message = validateUploadFile(file('large.png', 11 * 1024 * 1024), 'COVER')
  assert.match(message ?? '', /大小超出限制/)
  assert.match(message ?? '', /10 MB/)
})

test('rejects an unsupported extension and accepts a valid file', () => {
  assert.match(validateUploadFile(file('unsafe.svg', 1024), 'COVER') ?? '', /仅支持/)
  assert.equal(validateUploadFile(file('lesson.pdf', 1024), 'CONTENT'), undefined)
})
