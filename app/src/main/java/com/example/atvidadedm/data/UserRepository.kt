package com.example.atvidadedm.data

import com.example.atvidadedm.data.local.UserDao
import com.example.atvidadedm.data.local.UserEntity

class UserRepository(
    private val userDao: UserDao
) {
    suspend fun registerUser(
        name: String,
        email: String,
        phone: String,
        password: String
    ): RegisterResult {
        val existingUser = userDao.getByEmail(email.trim())
        if (existingUser != null) {
            return RegisterResult.EmailAlreadyExists
        }

        val rowId = userDao.insert(
            UserEntity(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                password = password
            )
        )

        return if (rowId > 0) RegisterResult.Success else RegisterResult.Failure
    }
}

sealed interface RegisterResult {
    data object Success : RegisterResult
    data object EmailAlreadyExists : RegisterResult
    data object Failure : RegisterResult
}

