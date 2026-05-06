package me.dcueto.zentrackapp.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

class ZenTrack : CliktCommand(name = "zentrack") {
    override fun help(context: Context) = "ZenTrack CLI"
    override fun run() = Unit
}

fun main(args: Array<String>) = ZenTrack()
    .subcommands(/* TODO: añadir comandos tasks, workspaces, sprints */)
    .main(args)
