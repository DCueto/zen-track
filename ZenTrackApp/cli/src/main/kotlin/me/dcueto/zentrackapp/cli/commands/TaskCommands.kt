package me.dcueto.zentrackapp.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class TaskCommands : CliktCommand(name = "tasks") {
    override fun help(context: Context) = "Gestiona tareas de un workspace"
    override fun run() = Unit
}

class TaskListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "Lista las tareas de un workspace"

    private val workspaceId by option("--workspace", "-w", help = "ID del workspace").required()

    override fun run() {
        // TODO Fase 2: taskService.getTasksByWorkspace(workspaceId)
        echo("tasks list --workspace $workspaceId — pendiente Fase 2")
    }
}

class TaskCreateCommand : CliktCommand(name = "create") {
    override fun help(context: Context) = "Crea una nueva tarea"

    private val workspaceId by option("--workspace", "-w", help = "ID del workspace").required()
    private val title by option("--title", "-t", help = "Título de la tarea").required()

    override fun run() {
        // TODO Fase 2: taskService.createTask(workspaceId, title)
        echo("tasks create --workspace $workspaceId --title \"$title\" — pendiente Fase 2")
    }
}

class TaskShowCommand : CliktCommand(name = "show") {
    override fun help(context: Context) = "Muestra el detalle de una tarea"

    private val taskId by option("--id", help = "Display ID de la tarea (ej. ZTK-42)").required()

    override fun run() {
        // TODO Fase 2: taskService.getTask(taskId)
        echo("tasks show --id $taskId — pendiente Fase 2")
    }
}
