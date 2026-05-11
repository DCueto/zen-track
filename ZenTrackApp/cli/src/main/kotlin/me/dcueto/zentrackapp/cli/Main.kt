package me.dcueto.zentrackapp.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context as cliktContext
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
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import java.io.File

class ZenTrack(replMode: Boolean = false) : CliktCommand(name = "zentrack") {
    init {
        // In REPL mode, prevent --help and errors from calling System.exit(),
        // so the loop continues after displaying help/error messages.
        if (replMode) this.cliktContext { exitProcess = { _ -> } }
    }

    override fun help(context: Context) = "ZenTrack CLI"
    override fun run() = Unit
}

fun buildRootCommand(session: ReplSession, replMode: Boolean = false): CliktCommand =
    ZenTrack(replMode).subcommands(
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

fun main(args: Array<String>) {
    val session = ReplSession()

    // One-shot / script mode: zentrack <command> [args...]
    if (args.isNotEmpty()) {
        buildRootCommand(session).main(args)
        return
    }

    // Interactive REPL mode
    val configDir = File(System.getProperty("user.home"), ".zentrack")
    configDir.mkdirs()

    val terminal = TerminalBuilder.builder().system(true).build()
    val parser = DefaultParser()
    val reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .history(DefaultHistory())
        .parser(parser)
        .variable(org.jline.reader.LineReader.HISTORY_FILE, configDir.resolve("history").absolutePath)
        .build()

    println("ZenTrack CLI  —  type 'exit' or Ctrl+D to quit, '--help' for commands")

    while (true) {
        val line = try {
            reader.readLine(session.prompt())
        } catch (e: EndOfFileException) {
            break
        } catch (e: UserInterruptException) {
            continue
        }

        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed == "exit" || trimmed == "quit") break

        val words = try {
            parser.parse(trimmed, trimmed.length).words()
        } catch (e: org.jline.reader.SyntaxError) {
            println("Parse error: ${e.message}")
            continue
        }

        if (words.isEmpty()) continue
        buildRootCommand(session, replMode = true).main(words.toTypedArray())
    }

    terminal.close()
    println("Bye!")
}
