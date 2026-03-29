package com.cointask.utils

import org.junit.Assert.*
import org.junit.Test

class PasswordUtilsTest {

    @Test
    fun `hashPassword returns different hashes for same password`() {
        // BCrypt uses salt, so same password should produce different hashes
        val password = "testPassword123"
        val hash1 = PasswordUtils.hashPassword(password)
        val hash2 = PasswordUtils.hashPassword(password)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hashPassword returns BCrypt format`() {
        val password = "testPassword123"
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$"))
    }

    @Test
    fun `verifyPassword returns true for correct password`() {
        val password = "testPassword123"
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(PasswordUtils.verifyPassword(password, hash))
    }

    @Test
    fun `verifyPassword returns false for incorrect password`() {
        val password = "testPassword123"
        val wrongPassword = "wrongPassword456"
        val hash = PasswordUtils.hashPassword(password)

        assertFalse(PasswordUtils.verifyPassword(wrongPassword, hash))
    }

    @Test
    fun `verifyPassword returns false for empty password`() {
        val password = "testPassword123"
        val hash = PasswordUtils.hashPassword(password)

        assertFalse(PasswordUtils.verifyPassword("", hash))
    }

    @Test
    fun `verifyPassword returns false for null hash`() {
        val password = "testPassword123"

        assertFalse(PasswordUtils.verifyPassword(password, ""))
    }

    @Test
    fun `verifyPassword returns false for invalid hash format`() {
        val password = "testPassword123"
        val invalidHash = "invalid_hash_format"

        assertFalse(PasswordUtils.verifyPassword(password, invalidHash))
    }

    @Test
    fun `isHashed returns true for BCrypt hash`() {
        val password = "testPassword123"
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(PasswordUtils.isHashed(hash))
    }

    @Test
    fun `isHashed returns false for plain text password`() {
        val plainPassword = "plainTextPassword"

        assertFalse(PasswordUtils.isHashed(plainPassword))
    }

    @Test
    fun `hashPassword handles special characters`() {
        val password = "P@ssw0rd!#$%^&*()"
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(PasswordUtils.verifyPassword(password, hash))
    }

    @Test
    fun `hashPassword handles unicode characters`() {
        val password = "密码🔐"
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(PasswordUtils.verifyPassword(password, hash))
    }

    @Test
    fun `hashPassword handles long passwords`() {
        val password = "a".repeat(100)
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(PasswordUtils.verifyPassword(password, hash))
    }
}
