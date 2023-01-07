package com.dokar.draggablemenu

import androidx.compose.runtime.Composable

interface DraggableMenuScope {
    fun item(content: @Composable () -> Unit)

    fun items(count: Int, itemContent: @Composable (Int) -> Unit)

    fun <T> items(items: Iterable<T>, itemContent: @Composable (T) -> Unit)
}

internal class DraggableMenuItemProvider : DraggableMenuScope {
    private val _itemContents: MutableList<@Composable () -> Unit> = mutableListOf()
    val itemsContents: List<@Composable () -> Unit> = _itemContents

    override fun item(content: @Composable () -> Unit) {
        _itemContents.add(content)
    }

    override fun items(count: Int, itemContent: @Composable (Int) -> Unit) {
        for (i in 0 until count) {
            _itemContents.add {
                itemContent(i)
            }
        }
    }

    override fun <T> items(items: Iterable<T>, itemContent: @Composable (T) -> Unit) {
        for (item in items) {
            _itemContents.add {
                itemContent(item)
            }
        }
    }
}
