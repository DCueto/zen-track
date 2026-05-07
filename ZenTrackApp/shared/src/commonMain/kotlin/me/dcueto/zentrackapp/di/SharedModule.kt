package me.dcueto.zentrackapp.di

import me.dcueto.zentrackapp.network.ProjectApiRepository
import me.dcueto.zentrackapp.network.WorkspaceApiRepository
import me.dcueto.zentrackapp.network.createHttpClient
import me.dcueto.zentrackapp.repository.ProjectRepository
import me.dcueto.zentrackapp.repository.WorkspaceRepository
import org.koin.dsl.module

val sharedModule = module {
    single { createHttpClient() }
    single<WorkspaceRepository> { WorkspaceApiRepository(get()) }
    single<ProjectRepository> { ProjectApiRepository(get()) }
}
