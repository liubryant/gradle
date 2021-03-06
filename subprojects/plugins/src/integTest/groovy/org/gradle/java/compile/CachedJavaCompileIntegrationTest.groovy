/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.java.compile

import org.gradle.AbstractCachedCompileIntegrationTest
import org.gradle.test.fixtures.file.TestFile

class CachedJavaCompileIntegrationTest extends AbstractCachedCompileIntegrationTest implements IncrementalCompileMultiProjectTestFixture {
    String compilationTask = ':compileJava'
    String compiledFile = "build/classes/main/Hello.class"

    def setupProjectInDirectory(TestFile project = temporaryFolder.testDirectory) {
        project.with {
            file('build.gradle').text = """
            plugins {
                id 'java'
                id 'application'
            }

            mainClassName = "Hello"

            repositories {
                mavenCentral()
            }

            dependencies {
                compile 'org.codehaus.groovy:groovy-all:2.4.9'
            }
        """.stripIndent()

        file('src/main/java/Hello.java') << """
            public class Hello {
                public static void main(String... args) {
                    System.out.println("Hello!");
                }
            }
        """.stripIndent()
        }
    }

    def "up-to-date incremental compilation is cached if nothing to recompile"() {
        given:
        buildFile.text = ""
        libraryAppProjectWithIncrementalCompilation()

        when:
        withBuildCache().succeeds appCompileJava

        then:
        executedAndNotSkipped appCompileJava

        when:
        writeUnusedLibraryClass()

        and:
        withBuildCache()
        executer.withArgument('-i')
        succeeds appCompileJava

        then:
        result.output.contains  "None of the classes needs to be compiled!"
        result.output.contains "${appCompileJava} UP-TO-DATE"
        executedAndNotSkipped libraryCompileJava

        when:
        withBuildCache().succeeds 'clean', appCompileJava

        then:
        result.output.contains "${appCompileJava} FROM-CACHE"
    }
}
