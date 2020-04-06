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

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
        def cp = classpath
        // it need to be resolved before switching context to another thread
        project.logger.info("[JvmRunWithJames]: Resolved classpath:" + cp.getFiles().join(':'))

        ExecutorService es = Executors.newSingleThreadExecutor()
        es.submit({
            try {
                project.logger.info("[JvmRunWithJames] Running ${appMain}")
                project.javaexec {
                    classpath = cp
                    main = appMain
                    jvmArgs = ["-javaagent:${jamesAgentJarPath}",
                               "-Djames.configurationPath=${jamesConfigurationPath}"]
                }
            }
            catch (Exception e) {
                project.logger.info("[JvmRunWithJames] Error: Task 'JvmRunWithJames' interrupted.", e)
                throw e;
            }
        } as Callable)
    }
}
