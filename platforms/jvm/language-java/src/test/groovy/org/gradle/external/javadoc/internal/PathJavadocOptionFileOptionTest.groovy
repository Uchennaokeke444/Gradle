/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.external.javadoc.internal

import spock.lang.Specification

class PathJavadocOptionFileOptionTest extends Specification {
    private JavadocOptionFileWriterContext writerContextMock = Mock()
    private final String optionName = "testOption"
    private final String joinBy = ";"

    private PathJavadocOptionFileOption pathOption = new PathJavadocOptionFileOption(optionName, new ArrayList<>(), joinBy)


    def testWriteNullValue() throws IOException {
        when:
        pathOption.write(writerContextMock)

        then:
        0 * writerContextMock._
    }

    def testWriteNoneNullValue() throws IOException {
        final File fileOne = new File("fileOne")
        final File fileTwo = new File("fileTwo")

        pathOption.getValue().add(fileOne)
        pathOption.getValue().add(fileTwo)

        when:
        pathOption.write(writerContextMock)

        then:
        1 * writerContextMock.writePathOption(optionName, pathOption.getValue(), joinBy)
    }
}
