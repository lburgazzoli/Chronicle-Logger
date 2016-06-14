/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.logger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.WireKey;
import org.jetbrains.annotations.NotNull;

public class ChronicleLogWriter implements Closeable {
    public static final Object[] EMPTY_OBJECT_ARRY = new Object[0];

    public enum EntryKey implements WireKey {
        LEVEL,
        TIMESTAMP,
        THREAD_NAME,
        LOGGER_NAME,
        MESSAGE,
        ARGS,
        EXCEPTION,
    }

    private final ChronicleQueue chronicleQueue;
    private final ThreadLocal<WeakReference<ExcerptAppender>> cache;

    public ChronicleLogWriter(@NotNull final ChronicleQueue chronicleQueue) {
        this.chronicleQueue = chronicleQueue;
        this.cache = new ThreadLocal<>();
    }

    @Override
    public void close() throws IOException {
        if (this.chronicleQueue != null) {
            this.chronicleQueue.close();
        }
    }

    public void write(
        final ChronicleLogLevel level,
        final long timestamp,
        final String threadName,
        final String loggerName,
        final String message) {

        getAppender().writeDocument(wire ->
            wire.write(EntryKey.LEVEL).asEnum(level)
                .write(EntryKey.TIMESTAMP).int64(timestamp)
                .write(EntryKey.THREAD_NAME).text(threadName)
                .write(EntryKey.LOGGER_NAME).text(loggerName)
                .write(EntryKey.MESSAGE).text(message)
                .write(EntryKey.ARGS).object(Object[].class, EMPTY_OBJECT_ARRY)
                .write(EntryKey.EXCEPTION).throwable(null)
        );
    }

    public void write(
        final ChronicleLogLevel level,
        final long timestamp,
        final String threadName,
        final String loggerName,
        final String message,
        final Throwable throwable) {
        getAppender().writeDocument(wire ->
            wire.write(EntryKey.LEVEL).asEnum(level)
                .write(EntryKey.TIMESTAMP).int64(timestamp)
                .write(EntryKey.THREAD_NAME).text(threadName)
                .write(EntryKey.LOGGER_NAME).text(loggerName)
                .write(EntryKey.MESSAGE).text(message)
                .write(EntryKey.ARGS).object(Object[].class, EMPTY_OBJECT_ARRY)
                .write(EntryKey.EXCEPTION).throwable(throwable)
        );
    }

    public void write(
        final ChronicleLogLevel level,
        final long timestamp,
        final String threadName,
        final String loggerName,
        final String message,
        final Throwable throwable,
        final Object arg1) {
        getAppender().writeDocument(wire ->
            wire.write(EntryKey.LEVEL).asEnum(level)
                .write(EntryKey.TIMESTAMP).int64(timestamp)
                .write(EntryKey.THREAD_NAME).text(threadName)
                .write(EntryKey.LOGGER_NAME).text(loggerName)
                .write(EntryKey.MESSAGE).text(message)
                .write(EntryKey.ARGS).array(w -> w.object(arg1), Object.class)
                .write(EntryKey.EXCEPTION).throwable(throwable)
        );
    }

    public void write(
        final ChronicleLogLevel level,
        final long timestamp,
        final String threadName,
        final String loggerName,
        final String message,
        final Throwable throwable,
        final Object arg1,
        final Object arg2) {
        getAppender().writeDocument(wire ->
            wire.write(EntryKey.LEVEL).asEnum(level)
                .write(EntryKey.TIMESTAMP).int64(timestamp)
                .write(EntryKey.THREAD_NAME).text(threadName)
                .write(EntryKey.LOGGER_NAME).text(loggerName)
                .write(EntryKey.MESSAGE).text(message)
                .write(EntryKey.ARGS).array(w -> { w.object(arg1); w.object(arg2); }, Object.class)
                .write(EntryKey.EXCEPTION).throwable(throwable)
        );
    }

    public void write(
        final ChronicleLogLevel level,
        final long timestamp,
        final String threadName,
        final String loggerName,
        final String message,
        final Throwable throwable,
        final Object[] args) {
        getAppender().writeDocument(wire ->
            wire.write(EntryKey.LEVEL).asEnum(level)
                .write(EntryKey.TIMESTAMP).int64(timestamp)
                .write(EntryKey.THREAD_NAME).text(threadName)
                .write(EntryKey.LOGGER_NAME).text(loggerName)
                .write(EntryKey.MESSAGE).text(message)
                .write(EntryKey.ARGS).object(Object[].class, args)
                .write(EntryKey.EXCEPTION).throwable(throwable)
        );
    }

    private ExcerptAppender getAppender() {
        WeakReference<ExcerptAppender> ref = this.cache.get();
        ExcerptAppender appender = ref != null ? ref.get() : null;

        if (appender == null) {
            appender = chronicleQueue.createAppender();
            cache.set(new WeakReference<>(appender));
        }

        return appender;
    }
}
