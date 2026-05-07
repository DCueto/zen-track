package me.dcueto.zentrackapp.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class SprintCommands : CliktCommand(name = "sprints") {
    override fun help(context: Context) = "Gestiona sprints de un workspace"
    override fun run() = Unit
}

class SprintListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "Lista los sprints de un workspace"

    private val workspaceId by option("--workspace", "-w", help = "ID del workspace").required()

    override fun run() {
        // TODO Fase 2: sprintService.getByWorkspace(workspaceId)
        echo("sprints list --workspace $workspaceId — pendiente Fase 2")
    }
}

class SprintCreateCommand : CliktCommand(name = "create") {
    override fun help(context: Context) = "Crea un nuevo sprint"

    private val workspaceId by option("--workspace", "-w", help = "ID del workspace").required()
    private val name by option("--name", "-n", help = "Nombre del sprint").required()

    override fun run() {
        // TODO Fase 2: sprintService.create(workspaceId, name)
        echo("sprints create --workspace $workspaceId --name \"$name\" — pendiente Fase 2")
    }
}
