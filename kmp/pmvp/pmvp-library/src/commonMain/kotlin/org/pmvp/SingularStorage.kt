package org.pmvp

import kotlinx.coroutines.flow.Flow

interface SingularStorage<T>: SingularImmutable<T>, SingularMutable<T>

interface SingularImmutable<T> {
    fun get(): Flow<T?>
}

interface SingularMutable<T> {
    fun update(model: T?): Flow<T?>
}
