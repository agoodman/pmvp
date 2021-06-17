package org.pmvp.ktor

import kotlinx.coroutines.flow.Flow

interface AuthTokenProvider {
    fun token(): Flow<String>
}
