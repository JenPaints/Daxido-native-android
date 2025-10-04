package com.daxido.core.auth

/**
 * FEATURE COMPLETE: Strong Password Validation
 * Ensures passwords meet security requirements
 */
object PasswordValidator {

    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 128

    /**
     * Validate password strength
     */
    fun validate(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()

        // Check length
        if (password.length < MIN_LENGTH) {
            errors.add("Password must be at least $MIN_LENGTH characters long")
        }
        if (password.length > MAX_LENGTH) {
            errors.add("Password must be less than $MAX_LENGTH characters")
        }

        // Check for uppercase letter
        if (!password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }

        // Check for lowercase letter
        if (!password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }

        // Check for digit
        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one number")
        }

        // Check for special character
        if (!password.any { !it.isLetterOrDigit() }) {
            errors.add("Password must contain at least one special character")
        }

        // Check for common patterns
        if (containsCommonPattern(password)) {
            errors.add("Password contains common patterns. Please choose a more unique password")
        }

        val strength = calculateStrength(password)

        return if (errors.isEmpty()) {
            PasswordValidationResult.Valid(strength)
        } else {
            PasswordValidationResult.Invalid(errors, strength)
        }
    }

    /**
     * Calculate password strength (0-100)
     */
    private fun calculateStrength(password: String): Int {
        var strength = 0

        // Length score (max 30 points)
        strength += minOf(password.length * 2, 30)

        // Character variety (max 40 points)
        if (password.any { it.isUpperCase() }) strength += 10
        if (password.any { it.isLowerCase() }) strength += 10
        if (password.any { it.isDigit() }) strength += 10
        if (password.any { !it.isLetterOrDigit() }) strength += 10

        // Entropy score (max 30 points)
        val uniqueChars = password.toSet().size
        strength += minOf(uniqueChars * 2, 30)

        // Penalize common patterns
        if (containsCommonPattern(password)) {
            strength -= 20
        }

        return strength.coerceIn(0, 100)
    }

    /**
     * Check for common password patterns
     */
    private fun containsCommonPattern(password: String): Boolean {
        val lowercasePassword = password.lowercase()

        // Common weak passwords
        val commonPasswords = listOf(
            "password", "12345678", "qwerty", "abc123", "letmein",
            "monkey", "dragon", "master", "welcome", "login"
        )

        if (commonPasswords.any { lowercasePassword.contains(it) }) {
            return true
        }

        // Sequential numbers
        if (password.contains(Regex("(012|123|234|345|456|567|678|789)"))) {
            return true
        }

        // Sequential letters
        if (lowercasePassword.contains(Regex("(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)"))) {
            return true
        }

        // Repeated characters
        if (password.contains(Regex("(.)\\1{2,}"))) {
            return true
        }

        return false
    }

    /**
     * Get user-friendly strength description
     */
    fun getStrengthDescription(strength: Int): String {
        return when {
            strength < 40 -> "Weak"
            strength < 60 -> "Fair"
            strength < 80 -> "Good"
            else -> "Strong"
        }
    }

    /**
     * Get strength color (for UI)
     */
    fun getStrengthColor(strength: Int): StrengthColor {
        return when {
            strength < 40 -> StrengthColor.RED
            strength < 60 -> StrengthColor.ORANGE
            strength < 80 -> StrengthColor.YELLOW
            else -> StrengthColor.GREEN
        }
    }
}

/**
 * Password validation result
 */
sealed class PasswordValidationResult {
    data class Valid(val strength: Int) : PasswordValidationResult()
    data class Invalid(val errors: List<String>, val strength: Int) : PasswordValidationResult()

    fun isValid(): Boolean = this is Valid
}

/**
 * Strength indicator color
 */
enum class StrengthColor {
    RED, ORANGE, YELLOW, GREEN
}
