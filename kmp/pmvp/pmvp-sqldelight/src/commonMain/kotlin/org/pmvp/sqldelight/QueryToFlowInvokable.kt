package org.pmvp.sqldelight

import com.squareup.sqldelight.Query
import kotlinx.coroutines.flow.Flow

/**
 * A helper invokable to do the transformation in the platform specific way.
 */
interface QueryToFlowInvokable {
    operator fun <T : Any> invoke(query: Query<T>): Flow<Query<T>>
}
