package com.jason.cloud.drive.utils

class ItemSelector<ITEM : ItemSelector.SelectableItem> {
    var isInSelectMode = false
    private val selectMap = HashMap<Any, Pair<ITEM, Boolean>>()
    private val onSelectListeners = arrayListOf<OnSelectListener<ITEM>>()

    fun reverseSelect(item: ITEM) {
        val isSelected = selectMap[item.primaryKey()]?.second != true
        selectMap[item.primaryKey()] = Pair(item, isSelected)
        val selectedList = selectMap.filter { it.value.second }.values.map { it.first }.toList()
        onSelectListeners.forEach {
            it.onSelectChanged(selectedList)
        }
    }

    fun serSelected(item: ITEM, selected: Boolean) {
        selectMap[item.primaryKey()] = Pair(item, selected)
        val selectedList = selectMap.filter { it.value.second }.values.map { it.first }.toList()
        onSelectListeners.forEach {
            it.onSelectChanged(selectedList)
        }
    }

    fun isSelected(item: ITEM): Boolean {
        return selectMap[item.primaryKey()]?.second == true
    }

    fun startSelect() {
        selectMap.clear()
        isInSelectMode = true
        onSelectListeners.forEach { it.onSelectStart() }
    }

    fun addOnSelectListener(listener: OnSelectListener<ITEM>) {
        this.onSelectListeners.add(listener)
    }

    fun cancelSelect() {
        selectMap.clear()
        isInSelectMode = false
        onSelectListeners.forEach { it.onSelectCanceled() }
    }

    interface SelectableItem {
        fun primaryKey(): Any
    }

    interface OnSelectListener<ITEM> {
        fun onSelectStart()

        fun onSelectCanceled()

        fun onSelectChanged(selects: List<ITEM>)
    }
}