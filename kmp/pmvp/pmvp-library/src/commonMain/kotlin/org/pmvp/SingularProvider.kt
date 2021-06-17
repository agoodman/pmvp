package org.pmvp

import kotlinx.coroutines.flow.Flow

interface SingularProvidable<T>: SingularMutable<T>, SingularImmutable<T>

open class SingularProvider<T>(
    protected val storage: SingularStorage<T>
): SingularProvidable<T> {

    override fun get(): Flow<T?> =
        storage.get()

    override fun update(model: T?): Flow<T?> =
        storage.update(model)

}
