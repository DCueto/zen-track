package me.dcueto.zentrackapp.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import me.dcueto.zentrackapp.cli.commands.SprintCommands
import me.dcueto.zentrackapp.cli.commands.SprintCreateCommand
import me.dcueto.zentrackapp.cli.commands.SprintListCommand
import me.dcueto.zentrackapp.cli.commands.TaskCommands
import me.dcueto.zentrackapp.cli.commands.TaskCreateCommand
import me.dcueto.zentrackapp.cli.commands.TaskListCommand
import me.dcueto.zentrackapp.cli.commands.TaskShowCommand
import me.dcueto.zentrackapp.cli.commands.WorkspaceCommands
import me.dcueto.zentrackapp.cli.commands.WorkspaceCreateCommand
import me.dcueto.zentrackapp.cli.commands.WorkspaceListCommand

class ZenTrack : CliktCommand(name = "zentrack") {
    override fun help(context: Context) = "ZenTrack CLI"
    override fun run() = Unit
}

fun main(args: Array<String>) = ZenTrack()
    .subcommands(
        TaskCommands().subcommands(
            TaskListCommand(),
            TaskCreateCommand(),
            TaskShowCommand()
        ),
        WorkspaceCommands().subcommands(
            WorkspaceListCommand(),
            WorkspaceCreateCommand()
        ),
        SprintCommands().subcommands(
            SprintListCommand(),
            SprintCreateCommand()
        )
    )
    .main(args)
