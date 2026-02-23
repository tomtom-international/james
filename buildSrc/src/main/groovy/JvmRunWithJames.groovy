/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

class JvmRunWithJames extends DefaultTask {

    @InputFiles
    FileCollection classpath

    @Input
    String appMain

    @Input
    String jamesAgentJarPath

    @Input
    String jamesConfigurationPath

    @TaskAction
    def runJvmWithJames() {
        def classpathString = classpath.files.collect { it.absolutePath }.join(File.pathSeparator)

        def javaExecutable = [System.getProperty('java.home'), 'bin', 'java'].join(File.separator)

        def command = [
            javaExecutable,
            "-javaagent:${jamesAgentJarPath}".toString(),
            "-Djames.configurationPath=${jamesConfigurationPath}".toString(),
            '-cp', classpathString,
            appMain
        ] as List<String>

        logger.info("[JvmRunWithJames] Starting: ${command.join(' ')}")

        def process = new ProcessBuilder(command)
            .directory(new File(project.projectDir.absolutePath))
            .redirectErrorStream(true)
            .start()

        logger.lifecycle("[JvmRunWithJames] Process started with PID ${process.pid()}")

        def taskLogger = logger
        Thread.startDaemon("james-output-pump") {
            process.inputStream.eachLine { line ->
                taskLogger.lifecycle("[james] ${line}")
            }
        }
    }
}
