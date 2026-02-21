package com.aqsama.neomarkor.di

import com.aqsama.neomarkor.data.local.StoragePreferences
import com.aqsama.neomarkor.data.repository.FileRepositoryImpl
import com.aqsama.neomarkor.domain.repository.FileRepository
import com.aqsama.neomarkor.presentation.viewmodel.DashboardViewModel
import com.aqsama.neomarkor.presentation.viewmodel.EditorViewModel
import com.aqsama.neomarkor.presentation.viewmodel.FileBrowserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single { StoragePreferences(androidContext()) }
    single<FileRepository> { FileRepositoryImpl(androidContext(), get(), get()) }
    viewModel { FileBrowserViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { params -> EditorViewModel(params.get<String>(), get()) }
}
