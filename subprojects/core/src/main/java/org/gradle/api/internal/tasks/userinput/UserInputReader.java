/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.internal.tasks.userinput;

public interface UserInputReader {
    void putInput(UserInput input);

    /**
     * Returns a string read from the input until a line-separator, or null if input was interrupted.
     */
    UserInput readInput();

    abstract class UserInput {
        abstract String getText();
    }

    UserInput END_OF_INPUT = new UserInput() {
        @Override
        String getText() {
            throw new IllegalStateException("No response available.");
        }
    };

    class TextResponse extends UserInput {
        private final String text;

        public TextResponse(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }
    }
}
