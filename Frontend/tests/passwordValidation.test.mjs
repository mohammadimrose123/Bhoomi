import assert from 'node:assert/strict'
import test from 'node:test'
import { getPasswordRequirements, passwordsMatch } from '../src/utils/passwordValidation.ts'

test('password confirmation rejects mismatches and empty confirmation', () => {
  assert.equal(passwordsMatch('Strong-Password42!', 'Strong-Password42'), false)
  assert.equal(passwordsMatch('Strong-Password42!', ''), false)
})

test('password confirmation accepts an exact match', () => {
  assert.equal(passwordsMatch('Strong-Password42!', 'Strong-Password42!'), true)
})

test('password strength feedback follows the reset password policy', () => {
  assert.deepEqual(getPasswordRequirements('weak'), [false, false, false, false])
  assert.deepEqual(getPasswordRequirements('Strong-Password42!'), [true, true, true, true])
})
