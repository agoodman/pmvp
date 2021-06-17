package org.pmvp.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

sealed class MutableListViewIntent<T> : ViewModelIntent {
    class Add<T>(val element: T) : MutableListViewIntent<T>()
    class Remove<T>(val element: T) : MutableListViewIntent<T>()
    class BringToFront<T>(val element: T) : MutableListViewIntent<T>()
    class SendToBack<T>(val element: T) : MutableListViewIntent<T>()
    class BringForward<T>(val element: T) : MutableListViewIntent<T>()
    class SendBackward<T>(val element: T) : MutableListViewIntent<T>()
    class Clear<T>() : MutableListViewIntent<T>()
}

@ExperimentalCoroutinesApi
@FlowPreview
open class MutableListViewModel<T>(
    val scope: CoroutineScope,
    initial: List<T> = emptyList()
) : ViewModelIntentConsumer<MutableListViewIntent<T>> {

    /**
     * Ordered list of elements
     */
    private val elementList: MutableList<T> = initial.toMutableList()

    /**
     * Channel of element list
     */
    private val elementChannel = ConflatedBroadcastChannel<List<T>>(initial)

    /**
     * Flow of element list
     */
    val elements: Flow<List<T>> = elementChannel.asFlow()

    override fun onIntent(intent: MutableListViewIntent<T>) {
        when (intent) {
            is MutableListViewIntent.Add -> addElement(intent.element)
            is MutableListViewIntent.Remove -> removeElement(intent.element)
            is MutableListViewIntent.BringToFront -> bringToFront(intent.element)
            is MutableListViewIntent.SendToBack -> sendToBack(intent.element)
            is MutableListViewIntent.BringForward -> bringForward(intent.element)
            is MutableListViewIntent.SendBackward -> sendBackward(intent.element)
            is MutableListViewIntent.Clear -> clear()
        }
    }

    private fun addElement(element: T) {
        elementList.add(elementList.count(), element)
        elementChannel.offer(elementList)
    }

    private fun removeElement(element: T) {
        elementList.remove(element)
        elementChannel.offer(elementList)
    }

    private fun bringToFront(element: T) {
        elementList.remove(element)
        elementList.add(elementList.count(), element)
        elementChannel.offer(elementList)
    }

    private fun sendToBack(element: T) {
        elementList.remove(element)
        elementList.add(0, element)
        elementChannel.offer(elementList)
    }

    private fun bringForward(element: T) {
        val index = elementList.indexOf(element)
        if( index < elementList.count() ) {
            elementList.remove(element)
            elementList.add(index + 1, element)
            elementChannel.offer(elementList)
        }
    }

    private fun sendBackward(element: T) {
        val index = elementList.indexOf(element)
        if( index > 0 ) {
            elementList.remove(element)
            elementList.add(index - 1, element)
            elementChannel.offer(elementList)
        }
    }

    private fun clear() {
        elementList.clear()
        elementChannel.offer(elementList)
    }

}
