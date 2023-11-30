/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import com.oracle.helidon.oci.identity.RepeatableInputStreamer.Configuration;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.identity.RepeatableInputStreamer.ReplayStream;
import static com.oracle.helidon.oci.identity.RepeatableInputStreamer.Stream;
import static com.oracle.helidon.oci.identity.RepeatableInputStreamer.create;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepeatableInputStreamTest {
    byte[] contents;

    @Test
    void sanity() throws Exception {
        String input = "\tHello\nWorld! ";
        Stream stream = create(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        String output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output, equalTo(input));
        stream.close();

        stream = create(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        ReplayStream replay = stream.replay();
        output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output, equalTo(input));
        output = new String(replay.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output, equalTo(input));
        stream.close();
        replay.close();
    }

    @Test
    void sanity_offline_encrypted() throws Exception {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("memoryThreshold", "1", "useEncryption", "true")))
                .build();
        Configuration configuration = RepeatableInputStreamer.loadConfig(config, true);

        String input = "\tHello\nWorld! ";
        Stream stream = create(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), configuration);
        String output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output, equalTo(input));
        stream.close();

        stream = create(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), configuration);
        ReplayStream replay = stream.replay();
        output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output, equalTo(input));
        output = new String(replay.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output, equalTo(input));
        stream.close();
        replay.close();
    }

    @Test
    void emptyStream_streamIsReadCompletelyFirst() throws Exception {
        Stream stream = create(new ByteArrayInputStream(new byte[] {}));
        assertThat(stream.available(), is(0));
        assertThat(stream.read(), is(-1));

        ReplayStream replay1 = stream.replay();
        ReplayStream replay2 = stream.replay();
        assertThat(replay1, not(sameInstance(replay2)));

        assertThat(replay1.available(), is(0));
        assertThat(replay1.read(), is(-1));

        assertThat(replay2.available(), is(0));
        assertThat(replay2.read(), is(-1));

        stream.close();
        replay1.close();
        replay2.close();

        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, replay1::read);
        assertThrows(IOException.class, replay2::read);
    }

    @Test
    void emptyStream_streamIsNotReadFirst() throws Exception {
        Stream stream = create(new ByteArrayInputStream(new byte[] {}));
//        assertThat(stream.available(), is(0));
//        assertThat(stream.read(), is(0));
//        assertThat(stream.read(), is(-1));

        ReplayStream replay1 = stream.replay();
        ReplayStream replay2 = stream.replay();
        assertThat(replay1, not(sameInstance(replay2)));

        assertThat(replay1.available(), is(0));
        assertThat(replay1.read(), is(-1));

        assertThat(replay2.available(), is(0));
        assertThat(replay2.read(), is(-1));

        // now we can finally read the first stream as shown in the commented out code above
        assertThat(stream.available(), is(0));
        assertThat(stream.read(), is(-1));

        stream.close();
        replay1.close();
        replay2.close();

        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, replay1::read);
        assertThrows(IOException.class, replay2::read);
    }

    @Test
    void boundedStream_0_streamIsReadCompletelyFirst() {
        boundedStream_n_streamIsReadCompletelyFirst(0);
    }

    @Test
    void boundedStream_0_streamIsNotReadFirst() {
        boundedStream_n_streamIsNotReadFirst(0);
    }

    @Test
    void boundedStream_1_streamIsReadCompletelyFirst() {
        boundedStream_n_streamIsReadCompletelyFirst(1);
    }

    @Test
    void boundedStream_1_streamIsNotReadFirst() {
        boundedStream_n_streamIsNotReadFirst(1);
    }

    @Test
    void boundedStream_4095_streamIsReadCompletelyFirst() {
        boundedStream_n_streamIsReadCompletelyFirst(RepeatableInputStreamer.DEFAULT_BYTES_IN_FIRST_BLOCK-1);
    }

    @Test
    void boundedStream_4095_streamIsNotReadFirst() {
        boundedStream_n_streamIsNotReadFirst(RepeatableInputStreamer.DEFAULT_BYTES_IN_FIRST_BLOCK-1);
    }

    @Test
    void boundedStream_4096_streamIsReadCompletelyFirst() {
        boundedStream_n_streamIsReadCompletelyFirst(RepeatableInputStreamer.DEFAULT_BYTES_IN_FIRST_BLOCK);
    }

    @Test
    void boundedStream_4096_streamIsNotReadFirst() {
        boundedStream_n_streamIsNotReadFirst(RepeatableInputStreamer.DEFAULT_BYTES_IN_FIRST_BLOCK);
    }

    @Test
    void boundedStream_4097_streamIsReadCompletelyFirst() {
        boundedStream_n_streamIsReadCompletelyFirst(RepeatableInputStreamer.DEFAULT_BYTES_IN_FIRST_BLOCK+1);
    }

    @Test
    void boundedStream_4097_streamIsNotReadFirst() {
        boundedStream_n_streamIsNotReadFirst(RepeatableInputStreamer.DEFAULT_BYTES_IN_FIRST_BLOCK+1);
    }

    @Test
    void boundedStream_limit_streamIsReadCompletelyFirst() {
        Config config = Config.builder()
                .sources(ConfigSources.create(
                        Map.of("memoryThreshold", "1", "streamThreshold", "5", "useEncryption", "false")))
                .build();
        Configuration cfg = RepeatableInputStreamer.loadConfig(config, true);
        assertThat(cfg.memoryThreshold(), is(1));
        assertThat(cfg.streamThreshold(), is(5L));
        assertThat(cfg.useEncryption(), is(false));

        boundedStream_n_streamIsReadCompletelyFirst(4, cfg);
        boundedStream_n_streamIsReadCompletelyFirst(5, cfg);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                                               () -> boundedStream_n_streamIsReadCompletelyFirst(6, cfg));
        assertThat(e.getMessage(), equalTo("read past streamThreshold: 5"));
    }

    @Test
    void boundedStream_limit_streamIsNotReadFirst() {
        Config config = Config.builder()
                .sources(ConfigSources.create(
                        Map.of("memoryThreshold", "4", "streamThreshold", "5", "useEncryption", "false")))
                .build();
        Configuration cfg = RepeatableInputStreamer.loadConfig(config, true);
        assertThat(cfg.memoryThreshold(), is(4));
        assertThat(cfg.streamThreshold(), is(5L));
        assertThat(cfg.useEncryption(), is(false));

        Optional<Path> backingPath = boundedStream_n_streamIsNotReadFirst(3, cfg);
        assertThat(backingPath.isPresent(), is(false));

        backingPath = boundedStream_n_streamIsNotReadFirst(4, cfg);
        assertThat(backingPath.isPresent(), is(true));

        backingPath = boundedStream_n_streamIsNotReadFirst(5, cfg);
        assertThat(backingPath.isPresent(), is(true));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                                               () -> boundedStream_n_streamIsNotReadFirst(6, cfg));
        assertThat(e.getMessage(), equalTo("read past streamThreshold: 5"));
    }

    @Test
    void nonEncryptedBackingStorage() {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("memoryThreshold", "1", "useEncryption", "false")))
                .build();
        Configuration cfg = RepeatableInputStreamer.loadConfig(config, true);
        assertThat(cfg.useEncryption(), is(false));

        Optional<Path> backingPath = boundedStream_n_streamIsReadCompletelyFirst("* Hello World!".getBytes(), null, cfg);
        assertThat(backingPath.isPresent(), is(true));

        String str = new String(contents, StandardCharsets.UTF_8);
        assertThat(str, str.contains("ello"), is(true));
    }

    @Test
    void encryptedBackingStorage() {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("memoryThreshold", "1", "useEncryption", "true")))
                .build();
        Configuration cfg = RepeatableInputStreamer.loadConfig(config, true);
        assertThat(cfg.useEncryption(), is(true));

        Optional<Path> backingPath = boundedStream_n_streamIsReadCompletelyFirst("* Hello World!".getBytes(), null, cfg);
        assertThat(backingPath.isPresent(), is(true));

        String str = new String(contents, StandardCharsets.UTF_8);
        assertThat(str, str.contains("ello"), is(false));
    }

    @Test
    void backingFilesAreDeletedWhenReferenceCountsGoToZero() throws Exception {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("memoryThreshold", "1", "useEncryption", "false")))
                .build();
        Configuration cfg = RepeatableInputStreamer.loadConfig(config, true);
        assertThat(cfg.useEncryption(), is(false));

        AtomicReference<ReplayStream> ref = new AtomicReference<>();
        Optional<Path> backingPath = boundedStream_n_streamIsReadCompletelyFirst("* Hello World!".getBytes(),
                                                                                 (stream) -> ref.set(stream.replay()),
                                                                                 cfg);
        assertThat(backingPath.isPresent(), is(true));
        assertThat(backingPath.get().toFile().exists(), is(true));

        ref.get().close();
        assertThat(backingPath.get().toFile().exists(), is(false));
    }

    @Test
    void badConfig() {
        {
            Config config = Config.builder()
                    .sources(ConfigSources.create(Map.of("memoryThreshold", "0", "streamThreshold", "10")))
                    .build();
            assertThrows(IllegalStateException.class, () -> RepeatableInputStreamer.loadConfig(config, true));
        }
        {
            Config config = Config.builder()
                    .sources(ConfigSources.create(Map.of("memoryThreshold", "100", "streamThreshold", "99")))
                    .build();
            assertThrows(IllegalStateException.class, () -> RepeatableInputStreamer.loadConfig(config, true));
        }
    }

    Optional<Path> boundedStream_n_streamIsReadCompletelyFirst(int size,
                                                               Configuration... cfg) {
        byte[] buff = createTestBytes(size);
        return boundedStream_n_streamIsReadCompletelyFirst(buff, null, cfg);
    }

    Optional<Path> boundedStream_n_streamIsReadCompletelyFirst(byte[] buff,
                                                               Consumer<Stream> streamConsumer,
                                                               Configuration... cfg) {
        Stream stream;
        if (cfg == null || cfg.length <= 0) {
            stream = create(new ByteArrayInputStream(buff));
        } else {
            stream = create(new ByteArrayInputStream(buff), cfg[0]);
        }

        if (streamConsumer != null) {
            streamConsumer.accept(stream);
        }

        try {
            {
                byte[] read = IOUtils.readFully(stream, buff.length);
                assertThat(Arrays.equals(buff, read), is(true));
            }

            // need to create a replay before we close
            ReplayStream replay1 = stream.replay();

            if (streamConsumer == null) {
                assertThat(stream.refCount(), is(2));
            }
            stream.close();
            if (streamConsumer == null) {
                assertThat(stream.refCount(), is(1));
            }

            {
                byte[] read = IOUtils.readFully(replay1, buff.length);
                assertThat(Arrays.equals(buff, read), is(true));
            }

            ReplayStream replay2 = stream.replay();
            assertThat(replay1, not(sameInstance(replay2)));
            {
                byte[] read = IOUtils.readFully(replay2, buff.length);
                assertThat(Arrays.equals(buff, read), is(true));
            }

            if (stream.backingStorage().isPresent()) {
                contents = Files.readAllBytes(stream.backingStorage().get());
            } else {
                contents = null;
            }

            if (streamConsumer == null) {
                assertThat(stream.refCount(), is(2));
            }
            replay1.close();
            if (streamConsumer == null) {
                assertThat(stream.refCount(), is(1));
            }
            replay2.close();
            if (streamConsumer == null) {
                assertThat(stream.refCount(), is(0));
            }

            assertThrows(IOException.class, stream::read);
            assertThrows(IOException.class, replay1::read);
            assertThrows(IOException.class, replay2::read);

            if (streamConsumer == null) {
                // throw extra closes in
                assertThat(stream.refCount(), is(0));
                stream.close();
                assertThat(stream.refCount(), is(0));
                replay1.close();
                assertThat(stream.refCount(), is(0));
                replay2.close();
                assertThat(stream.refCount(), is(0));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(stream.backingStorage().toString(), e);
        }

        return stream.backingStorage();
    }

    static Optional<Path> boundedStream_n_streamIsNotReadFirst(int size,
                                                               Configuration... cfg) {
        byte[] buff = createTestBytes(size);
        Stream stream;
        if (cfg == null || cfg.length <= 0) {
            stream = create(new ByteArrayInputStream(buff));
        } else {
            stream = create(new ByteArrayInputStream(buff), cfg[0]);
        }
        ReplayStream replay1 = stream.replay();
        ReplayStream replay2 = stream.replay();
        assertThat(replay1, not(sameInstance(replay2)));

        try {
            {
                byte[] read = IOUtils.readFully(stream, buff.length);
                assertThat(Arrays.equals(buff, read), is(true));
            }
            {
                byte[] read = IOUtils.readFully(replay1, buff.length);
                assertThat(Arrays.equals(buff, read), is(true));
            }
            {
                byte[] read = IOUtils.readFully(replay2, buff.length);
                assertThat(Arrays.equals(buff, read), is(true));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertThat(stream.refCount(), is(3));
        replay1.close();
        assertThat(stream.refCount(), is(2));
        replay2.close();
        assertThat(stream.refCount(), is(1));
        stream.close();
        assertThat(stream.refCount(), is(0));

        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, replay1::read);
        assertThrows(IOException.class, replay2::read);

        // throw extra closes in
        assertThat(stream.refCount(), is(0));
        stream.close();
        assertThat(stream.refCount(), is(0));
        replay1.close();
        assertThat(stream.refCount(), is(0));
        replay2.close();
        assertThat(stream.refCount(), is(0));

        return stream.backingStorage();
    }

    static byte[] createTestBytes(int size) {
        byte[] buff = new byte[size];
        for (int i = 0; i < size; i++) {
            buff[i] = (byte) (i % 255);
        }
        return buff;
    }

}
