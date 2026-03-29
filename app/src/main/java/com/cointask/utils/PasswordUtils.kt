package com.cointask.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordUtils {

    private const val BCRYPT_ROUNDS = 10

    /**
     * Hash a plain text password using BCrypt
     */
    fun hashPassword(plainPassword: String): String {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS))
    }

    /**
     * Verify a plain text password against a hashed password
     * @return true if password matches, false otherwise
     */
    fun verifyPassword(plainPassword: String, hashedPassword: String): Boolean {
        return try {
            BCrypt.checkpw(plainPassword, hashedPassword)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Check if a password is already hashed (starts with $2a$)
     */
    fun isHashed(password: String): Boolean {
        return password.startsWith("$2a$") || password.startsWith("$2b$")
    }
}
