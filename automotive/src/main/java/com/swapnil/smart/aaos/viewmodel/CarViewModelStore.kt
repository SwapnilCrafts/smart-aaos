package com.swapnil.smart.aaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

object CarViewModelStore : ViewModelStoreOwner {

    private val store = ViewModelStore()

    override val viewModelStore: ViewModelStore = store

    fun <T : ViewModel> get(clazz: Class<T>): T {
        return ViewModelProvider(this)[clazz]
    }

    // ✅ Call this when app is destroyed to clear all ViewModels
    fun clear() {
        store.clear()
    }
}