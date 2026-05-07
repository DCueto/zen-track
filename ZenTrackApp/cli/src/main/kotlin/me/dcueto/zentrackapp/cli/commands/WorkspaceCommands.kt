package me.dcueto.zentrackapp.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class WorkspaceCommands : CliktCommand(name = "workspaces") {
    override fun help(context: Context) = "Gestiona workspaces"
    override fun run() = Unit
}

class WorkspaceListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "Lista todos los workspaces del usuario"

    override fun run() {
        // TODO Fase 2: workspaceService.getAll()
        echo("workspaces list — pendiente Fase 2")
    }
}

class WorkspaceCreateCommand : CliktCommand(name = "create") {
    override fun help(context: Context) = "Crea un nuevo workspace"

    private val name by option("--name", "-n", help = "Nombre del workspace").required()

    override fun run() {
        // TODO Fase 2: workspaceService.create(name)
        echo("workspaces create --name \"$name\" — pendiente Fase 2")
    }
}
