/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.secret;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ShutdownHookBean {

    private static final List<Consumer<Object>> shutDownHooks = new ArrayList<>();
    private static final ReentrantLock lock = new ReentrantLock();

    static void addShutdownHook(Consumer<Object> shutdownHook) {
        lock.lock();
        try {
            shutDownHooks.add(shutdownHook);
        } finally {
            lock.unlock();
        }
    }

    private void rightBeforeShutdown(@Observes @BeforeDestroyed(ApplicationScoped.class) final Object event) {
        lock.lock();
        try {
            shutDownHooks.forEach(h -> h.accept(event));
        } finally {
            lock.unlock();
        }
    }
}
