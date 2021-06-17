package org.pmvp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest

@FlowPreview
@ExperimentalCoroutinesApi
class SingularStorageTest {

    @Test
    fun testGet() = runBlockingTest {
        val storage = SingularItemStorage()
        val provider = SingularItemProvider(storage)
        assertNull(provider.get().first())
    }

    @Test
    fun testUpdate() = runBlockingTest {
        val storage = SingularItemStorage()
        val provider = SingularItemProvider(storage)
        val expectedItem = SingularItem("item")
        provider.update(expectedItem).first()
        val actualItem = provider.get().first()
        assertNotNull(actualItem, "expected item")
        assertEquals(expectedItem.value, actualItem.value, "wrong value")
    }

}
