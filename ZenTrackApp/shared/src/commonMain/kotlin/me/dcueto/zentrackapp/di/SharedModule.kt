package me.dcueto.zentrackapp.di

import me.dcueto.zentrackapp.network.createHttpClient
import org.koin.dsl.module

val sharedModule = module {
    single { createHttpClient() }
}
