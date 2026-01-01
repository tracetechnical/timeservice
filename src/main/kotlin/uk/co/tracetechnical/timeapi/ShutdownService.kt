package uk.co.tracetechnical.timeapi

import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import kotlin.system.exitProcess

@Service
class ShutdownService(
    private val context: ConfigurableApplicationContext
) {
    fun shutdown(exitCodeInt: Int) {
        val exitCode = SpringApplication.exit(
            context,
            ExitCodeGenerator { exitCodeInt }
        )
        exitProcess(exitCode)
    }
}
