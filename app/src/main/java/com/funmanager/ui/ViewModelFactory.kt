package com.funmanager.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.funmanager.AppContainer
import com.funmanager.FunManagerApp

/** Lấy [AppContainer] trong `viewModelFactory { initializer { ... } }`. */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as FunManagerApp).container
