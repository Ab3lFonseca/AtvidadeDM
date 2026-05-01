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

    suspend fun authenticateUser(
        email: String,
        password: String
    ): LoginResult {
        val user = userDao.authenticate(
            email = email.trim(),
            password = password
        )

        return if (user != null) {
            LoginResult.Success(user)
        } else {
            LoginResult.InvalidCredentials
        }
    }

    suspend fun findUserByEmail(email: String): UserEntity? {
        return userDao.getByEmail(email.trim())
    }
}

sealed interface RegisterResult {
    data object Success : RegisterResult
    data object EmailAlreadyExists : RegisterResult
    data object Failure : RegisterResult
}

sealed interface LoginResult {
    data class Success(val user: UserEntity) : LoginResult
    data object InvalidCredentials : LoginResult
}

