package com.cointask.data.models

import org.junit.Assert.*
import org.junit.Test

class UserModelTest {

    @Test
    fun `User creation with valid data`() {
        val user = User(
            id = 1,
            email = "test@example.com",
            password = "hashedPassword123",
            fullName = "Test User",
            role = UserRole.USER,
            coins = 100,
            isVerified = true,
            isActive = true,
            isSuspended = false,
            createdAt = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis()
        )

        assertEquals(1, user.id)
        assertEquals("test@example.com", user.email)
        assertEquals(UserRole.USER, user.role)
        assertEquals(100, user.coins)
        assertTrue(user.isVerified)
        assertTrue(user.isActive)
        assertFalse(user.isSuspended)
    }

    @Test
    fun `User equals and hashCode work correctly`() {
        val user1 = User(email = "test@example.com", password = "pass", fullName = "Test", role = UserRole.USER)
        val user2 = User(email = "test@example.com", password = "pass", fullName = "Test", role = UserRole.USER)

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun `User toString contains email`() {
        val user = User(email = "test@example.com", password = "pass", fullName = "Test", role = UserRole.USER)
        val toString = user.toString()

        assertTrue(toString.contains("test@example.com"))
        assertTrue(toString.contains("Test"))
    }

    @Test
    fun `UserRole values are correct`() {
        assertEquals("USER", UserRole.USER.name)
        assertEquals("ADVERTISER", UserRole.ADVERTISER.name)
        assertEquals("ADMIN", UserRole.ADMIN.name)
    }
}
