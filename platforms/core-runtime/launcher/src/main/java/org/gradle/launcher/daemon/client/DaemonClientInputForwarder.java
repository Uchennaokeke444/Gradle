/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.launcher.daemon.client;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.dispatch.Dispatch;
import org.gradle.internal.io.TextStream;
import org.gradle.internal.logging.console.DefaultUserInput;
import org.gradle.internal.logging.console.UserInput;
import org.gradle.launcher.daemon.protocol.CloseInput;
import org.gradle.launcher.daemon.protocol.ForwardInput;
import org.gradle.launcher.daemon.protocol.InputMessage;
import org.gradle.launcher.daemon.protocol.UserResponse;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Eagerly consumes from an input stream, sending line by line ForwardInput
 * commands over the connection and finishing with a CloseInput command.
 * It also listens to cancel requests and forwards it too as Cancel command.
 */
public class DaemonClientInputForwarder implements Stoppable {
    private static final Logger LOGGER = Logging.getLogger(DaemonClientInputForwarder.class);

    public static final int DEFAULT_BUFFER_SIZE = 8192;
    private final InputForwarder forwarder;

    public DaemonClientInputForwarder(
        InputStream inputStream,
        Dispatch<? super InputMessage> dispatch,
        DefaultUserInput userInput,
        ExecutorFactory executorFactory
    ) {
        this(inputStream, dispatch, userInput, executorFactory, DEFAULT_BUFFER_SIZE);
    }

    public DaemonClientInputForwarder(
        InputStream inputStream,
        Dispatch<? super InputMessage> dispatch,
        DefaultUserInput userInput,
        ExecutorFactory executorFactory,
        int bufferSize
    ) {
        ForwardTextStreamToConnection handler = new ForwardTextStreamToConnection(dispatch);
        forwarder = new InputForwarder(inputStream, handler, executorFactory, bufferSize);
        userInput.delegateTo(new ForwardingUserInput(handler));
    }

    public void start() {
        forwarder.start();
    }

    @Override
    public void stop() {
        forwarder.stop();
    }

    private static class ForwardingUserInput implements UserInput {
        private final ForwardTextStreamToConnection handler;

        public ForwardingUserInput(ForwardTextStreamToConnection handler) {
            this.handler = handler;
        }

        @Override
        public void forwardResponse() {
            handler.forwardResponse();
        }
    }

    private static class ForwardTextStreamToConnection implements TextStream {
        private final Dispatch<? super InputMessage> dispatch;
        private final AtomicBoolean forwardResponse = new AtomicBoolean();

        public ForwardTextStreamToConnection(Dispatch<? super InputMessage> dispatch) {
            this.dispatch = dispatch;
        }

        void forwardResponse() {
            forwardResponse.set(true);
        }

        @Override
        public void text(String input) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Forwarding input to daemon: '{}'", input.replace("\n", "\\n"));
            }
            if (forwardResponse.compareAndSet(true, false)) {
                dispatch.dispatch(new UserResponse(input));
            } else {
                dispatch.dispatch(new ForwardInput(input.getBytes()));
            }
        }

        @Override
        public void endOfStream(@Nullable Throwable failure) {
            CloseInput message = new CloseInput();
            LOGGER.debug("Dispatching close input message: {}", message);
            dispatch.dispatch(message);
        }
    }
}
