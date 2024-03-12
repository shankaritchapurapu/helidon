/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import jakarta.enterprise.inject.spi.CDI;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;

import org.apache.commons.io.IOUtils;

/**
 * This class provides an ephemeral stream (available to only this JVM instance) that allows for storage (via temporary files)
 * that enables a {@link java.io.InputStream} to be repeatedly read.
 * <p>
 * The usage pattern is:
 * <ol>
 *  <li> InputStream stream = RepeatableInputStreamer.create(inputStream, RepeatableInputStreamer.loadConfig())
 *  <li>  ... optionally, read the stream ...
 *  <li>  ... at any time call stream.replay() - this will return a cloned stream that will start reading from the stream at the same position as when it was found on step 1.
 *  <li>  ... close the stream(s)
 * </ol>
 *
 *  Note that the backing storage will be deleted explicitly when the stream and all replay instances are closed (or read fully). If
 *  the streams are not closed then the backing files will be deleted on normal JVM exit - but it is highly advisable to not depend
 *  upon this since it will risk memory and disk exhaustion.
 */
class RepeatableInputStreamer {
    private static final Logger LOGGER = Logger.getLogger(RepeatableInputStreamer.class.getName());

    static final String DEFAULT_CONFIG_KEY = "repeatable-input-streamer";
    static final int DEFAULT_BYTES_IN_FIRST_BLOCK = 8 * 1024;
    static final long DEFAULT_BYTES_MAX = 256 * 1024 * 1024;
    static final boolean DEFAULT_USE_FILESYSTEM = true;
    static final boolean DEFAULT_USE_ENCRYPTION = false;
    static final String DEFAULT_ENCRYPTION_ALGORITHM = "AES";
    static final String DEFAULT_ENCRYPTION_CIPHER = "AES/CBC/PKCS5Padding";

    private static boolean LOGGED;

    private static final LazyValue<MetricsHelper> metricsHelper = LazyValue.create(() -> CDI.current().getBeanManager().getExtension(
            MetricsHelper.class));


    private RepeatableInputStreamer() {
    }

    /**
     * Called {@link #create(java.io.InputStream, com.oracle.helidon.oci.identity.RepeatableInputStreamer.Configuration)} using
     * the {@link com.oracle.helidon.oci.identity.RepeatableInputStreamer.Configuration} loaded from {@link #loadConfig()}.

     * @return the offline-capable, read-repeatable stream
     */
    public static Stream create(InputStream stream) {
        return create(stream, loadConfig());
    }

    /**
     * Creates an offline-capable, read-repeatable {@link java.io.InputStream}.
     *
     * @param stream the original stream
     * @param config the configuration to apply
     * @return the offline-capable, read-repeatable stream
     */
    public static Stream create(InputStream stream,
                                Configuration config) {
        try {
            File tempFile = null;
            OutputStream offlineOut = null;
            int size = Math.min(config.memoryThreshold() - 1, stream.available()) + 1;
            byte[] firstBlockIn = new byte[size];
            int read = stream.readNBytes(firstBlockIn, 0, size - 1);
            int oneMoreByte = stream.read();
            if (oneMoreByte == IOUtils.EOF) {
                size--;
            } else {
                firstBlockIn[read] = Integer.valueOf(oneMoreByte).byteValue();

                if (config.useFilesystem()) {
                    // we know there is a potential for more to come...
                    tempFile = Files.createTempFile(RepeatableInputStreamer.class.getSimpleName(), ".tmp").toFile();
                    tempFile.deleteOnExit();
                    offlineOut = new FileOutputStream(tempFile);

                    if (config.useEncryption()) {
                        Cipher cipher = config.encCipher.get();
                        offlineOut = new CipherOutputStream(offlineOut, cipher);
                    }
                }
            }

            return new Stream(config, firstBlockIn, size, stream, tempFile, offlineOut);
        } catch (IOException e) {
            metricsHelper.get().repeatableStreamExceptions().inc();
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads the default {@link com.oracle.helidon.oci.identity.RepeatableInputStreamer.Configuration} named
     * {@link #DEFAULT_CONFIG_KEY}, and will use intelligent default values if the configuration does not exist in the
     * backing config subsystem.
     *
     * @return a configuration object
     */
    public static Configuration loadConfig() {
        return loadConfig(OciIdentityConfiguration
                                  .globalMpConfig().get(DEFAULT_CONFIG_KEY), false);
    }

    /**
     * Loads the names configuration.
     *
     * @param config                the config
     * @param expectedToBePresent   flag indicating whether the config key is expected to exist
     * @return a configuration object
     */
    public static Configuration loadConfig(Config config,
                                           boolean expectedToBePresent) {
        if (expectedToBePresent && !config.exists()) {
            throw new ConfigException("The configKey `" + config.key() + "` was expected to be found.");
        }
        return new Configuration(config).validated();
    }


    /**
     * The configuration governing this feature's behavior.
     */
    public static class Configuration {
        private final LazyValue<SecretKey> secretKey = LazyValue.create(this::createSecretKey);
        private final LazyValue<Cipher> encCipher = LazyValue.create(() -> createCipher(secretKey.get(), Cipher.ENCRYPT_MODE));
        private final LazyValue<Cipher> decCipher = LazyValue.create(() -> createCipher(secretKey.get(), Cipher.DECRYPT_MODE));

        private final Config config;

        Configuration(Config config) {
            this.config = config;
        }

        /**
         * This is the memory threshold to buffer into before streaming to the offline storage medium. The default value is
         * {@link #DEFAULT_BYTES_IN_FIRST_BLOCK}.
         *
         * @return the memory threshold prior to streaming to offline storage
         */
        public int memoryThreshold() {
            int result = config.get("memoryThreshold").asInt().orElse(DEFAULT_BYTES_IN_FIRST_BLOCK);
            if (result < 1) {
                throw new IllegalStateException("`memoryThreshold` should be configured greater than 1; val=" + result);
            }
            return result;
        }

        /**
         * This is the threshold limit for what will be tolerated to stream before an {@link java.io.IOException} will be thrown.
         * The default value is {@link #DEFAULT_BYTES_MAX}.
         *
         * @return the streaming threshold limit
         */
        public long streamThreshold() {
            long result = config.get("streamThreshold").asLong().orElse(DEFAULT_BYTES_MAX);
            if (result < memoryThreshold()) {
                throw new IllegalStateException("`streamThreshold` must be larger than `memoryThreshold`; val=" + result);
            }
            return result;
        }

        /**
         * This flag indicates whether any offline storage should be enabled. The default value is {@link #DEFAULT_USE_FILESYSTEM}.
         *
         * @return the flag indicating whether offline temp file storage will be enabled
         */
        public boolean useFilesystem() {
            return config.get("useFilesystem").asBoolean().orElse(DEFAULT_USE_FILESYSTEM);
        }

        /**
         * This flag indicates whether any offline storage should be encrypted. The default value is {@link #DEFAULT_USE_ENCRYPTION}.
         * This is only applicable when {@link #useFilesystem()} is enabled.
         *
         * @return the flag indicating whether encryption is in use
         */
        public boolean useEncryption() {
            return config.get("useEncryption").asBoolean().orElse(DEFAULT_USE_ENCRYPTION);
        }

        /**
         * This is the encryption algorithm that is used when {@link #useEncryption()} is enabled. The default value
         * is {@link #DEFAULT_ENCRYPTION_ALGORITHM}.
         *
         * @return the encryption algorithm
         */
        public String encryptionAlgorithm() {
            return config.get("encryptionAlgorithm").asString().orElse(DEFAULT_ENCRYPTION_ALGORITHM);
        }

        /**
         * This is the encryption cipher that is used when {@link #useEncryption()} is enabled. The default value
         * is {@link #DEFAULT_ENCRYPTION_CIPHER}.
         *
         * @return the encryption cipher
         */
        public String encryptionCipher() {
            return config.get("encryptionCipher").asString().orElse(DEFAULT_ENCRYPTION_CIPHER);
        }

        @Override
        public String toString() {
            return "{\tmemoryThreshold: " + memoryThreshold()
                    + ";\n\tuseFilesystem: " + useFilesystem()
                    + ";\n\tuseEncryption: " + useEncryption()
                    + ";\n\tstreamThreshold: " + streamThreshold()
                    + "\n}";
        }

        /**
         * Will validate the integrity of the configuration and then return {@code this}.
         *
         * @return the validated configuration
         */
        public Configuration validated() {
            streamThreshold();
            return this;
        }

        private SecretKey createSecretKey() {
            try {
                return KeyGenerator.getInstance(encryptionAlgorithm()).generateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private Cipher createCipher(SecretKey secretKey,
                                    int mode) {
            try {
                Cipher result = Cipher.getInstance(encryptionCipher());
                if (mode == Cipher.ENCRYPT_MODE) {
                    result.init(mode, secretKey);
                } else if (mode == Cipher.DECRYPT_MODE) {
                    result.init(mode, secretKey, new IvParameterSpec(encCipher.get().getIV()));
                } else {
                    throw new IllegalStateException("invalid mode: " + mode);
                }
                return result;
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    /**
     * The wrapped input stream.
     */
    public static class Stream extends InputStream {
        private final AtomicInteger refCount = new AtomicInteger(1);
        private final Configuration configuration;
        private final long streamThreshold;
        private final File tempFile;
        private byte[] firstBlockIn;
        private final int firstBlockInRealLength;
        private final InputStream remainingIn;
        private transient OutputStream offlineOut;
        private transient long readPos;

        Stream(Configuration configuration,
               byte[] firstBlockIn,
               int firstBlockInRealLength,
               InputStream remainingIn,
               File tempFile,
               OutputStream offlineOut) {
            this.configuration = configuration;
            this.streamThreshold = configuration.streamThreshold();
            this.firstBlockIn = firstBlockIn;
            this.firstBlockInRealLength = firstBlockInRealLength;
            this.remainingIn = remainingIn;
            this.tempFile = tempFile;
            this.offlineOut = offlineOut;

            if (!LOGGED) {
                LOGGED = true;
                LOGGER.log(Level.FINE, "configuration: " + configuration);
            }
        }

        @Override
        public synchronized int read() throws IOException {
            try {
                if (readPos < 0) {
                    throw new IOException("stream is closed");
                } else if (firstBlockIn.length == 0) {
                    return IOUtils.EOF;
                } else if (readPos < firstBlockInRealLength) {
                    return (int) firstBlockIn[(int) readPos++];
                } else if (readPos >= streamThreshold) {
                    IllegalStateException e = new IllegalStateException("read past streamThreshold: " + readPos);
                    close();
                    throw e;
                } else if (offlineOut == null) {
                    int available = remainingIn.available();
                    if (available > 0) {
                        // this is an insane state since there is more to read
                        throw new IllegalStateException();
                    }
                    return IOUtils.EOF;
                }

                int byteRead = remainingIn.read();
                readPos++;
                if (byteRead == IOUtils.EOF) {
                    IOUtils.closeQuietly(offlineOut);
                    return IOUtils.EOF;
                }
                offlineOut.write(byteRead);

                return byteRead;
            } catch (Exception ex) {
                metricsHelper.get().repeatableStreamExceptions().inc();
                throw ex;
            }
        }

        @Override
        public synchronized void close() {
            if (tempFile == null) {
                metricsHelper.get().repeatableStreamInMemory().inc();
            } else {
                metricsHelper.get().repeatableStreamFileUsage().update(readPos);
            }
            if (offlineOut != null) {
                IOUtils.closeQuietly(offlineOut);
                offlineOut = null;
            }

            if (readPos != IOUtils.EOF) {
                readPos = IOUtils.EOF;
                deref();
            }

            // optimization
            firstBlockIn = null;
        }

        /**
         * Allows this {@link Stream} to be replayed from the point where {@link #create(java.io.InputStream, com.oracle.helidon.oci.identity.RepeatableInputStreamer.Configuration)}
         * was called.
         *
         * @return a replay stream
         */
        public synchronized ReplayStream replay() {
            if (readPos < 0) {
                throw new IllegalStateException("Can't replay after EOF or close");
            }
            assert(firstBlockIn != null);

            if (tempFile == null) {
                refCount.incrementAndGet();
                return new ReplayStream(firstBlockIn, firstBlockInRealLength, null, this::deref);
            }

            try {
                refCount.incrementAndGet();

                // flush for good luck
                if (offlineOut != null) {
                    offlineOut.flush();
                }

                InputStream offlineIn = new FileInputStream(tempFile);
                if (configuration.useEncryption()) {
                    offlineIn = new CipherInputStream(offlineIn, configuration.decCipher.get());
                }

                return new ReplayStream(firstBlockIn, firstBlockInRealLength, offlineIn, this::deref);
            } catch (IOException e) {
                metricsHelper.get().repeatableStreamExceptions().inc();
                refCount.decrementAndGet();
                throw new UncheckedIOException(e);
            }
        }

        Optional<Path> backingStorage() {
            return Optional.ofNullable(tempFile != null ? tempFile.toPath() : null);
        }

        int refCount() {
            return refCount.get();
        }

        void deref() {
            if (refCount.decrementAndGet() <= 0
                    && tempFile != null) {
                tempFile.delete();
            }
        }

        Configuration configuration() {
            return configuration;
        }
    }


    /**
     * The replayable stream.
     */
    public static class ReplayStream extends InputStream {
        private byte[] firstBlockIn;
        private final int firstBlockInRealLength;
        private final InputStream offlineIn;
        private transient Runnable closeable;
        private transient long readPos;

        ReplayStream(byte[] firstBlockIn,
                     int firstBlockInRealLength,
                     InputStream offlineIn,
                     Runnable closeable) {
            this.firstBlockIn = firstBlockIn;
            this.firstBlockInRealLength = firstBlockInRealLength;
            this.offlineIn = offlineIn;
            this.closeable = closeable;
        }

        @Override
        public synchronized int read() throws IOException {
            if (readPos < 0) {
                throw new IOException("stream is closed");
            } else if (firstBlockInRealLength <= 0) {
                return IOUtils.EOF;
            } else if (readPos < firstBlockInRealLength) {
                return (int) firstBlockIn[(int) readPos++];
            }

            int byteRead = (offlineIn == null) ? IOUtils.EOF : offlineIn.read();
            readPos++;
            return byteRead;
        }

        @Override
        public synchronized void close() {
            IOUtils.closeQuietly(offlineIn);
            readPos = IOUtils.EOF;

            if (closeable != null) {
                closeable.run();
                closeable = null;
            }

            // optimization
            firstBlockIn = null;
        }
    }

}
