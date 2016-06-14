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


import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import net.openhft.chronicle.core.threads.EventLoop;
import net.openhft.chronicle.core.time.TimeProvider;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.queue.impl.single.StoreRecoveryFactory;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChronicleLogAppenderConfig {

    private static final String[] KEYS = new String[] {
        "buffered",
        "blockSize",
        "bufferCapacity",
        "rollCycle",
        "epoch",
        "eventLoop",
        "pauserSupplier",
        "indexCount",
        "indexSpacing",
        "wireType"
    };

    private final SingleChronicleQueueBuilder builder;

    public ChronicleLogAppenderConfig() {
        // TODO: check what is the best way to create it
        this.builder = new SingleChronicleQueueBuilder((File)null);
    }

    // *************************************************************************
    //
    // *************************************************************************

    public boolean isBuffered() {
        return this.builder.buffered();
    }

    public void setBuffred(boolean buffered) {
        this.builder.buffered(buffered);
    }

    public int getBlockSize() {
        return (int)this.builder.blockSize();
    }

    // TODO: check method parameter, should be long
    public void setBlockSize(int blockSize) {
        this.builder.blockSize(blockSize);
    }

    public long getBufferCapacity() {
        return this.builder.bufferCapacity();
    }

    public void setBufferCapacity(long bufferCapacity) {
        this.builder.bufferCapacity(bufferCapacity);
    }

    public RollCycle getRollCycle() {
        return this.builder.rollCycle();
    }

    public void setRollCycle(RollCycle rollCycle) {
        this.builder.rollCycle(rollCycle);
    }

    public void setRollCycle(String rollCycle) {
        this.builder.rollCycle(RollCycles.valueOf(rollCycle));
    }

    public long getEpoch() {
        return this.builder.epoch();
    }

    public void setEpoch(long epoch) {
        this.builder.epoch(epoch);
    }

    public EventLoop getEventLoop() {
        return this.builder.eventLoop();
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.builder.eventLoop(eventLoop);
    }

    public Supplier<Pauser> getPauserSupplier() {
        return this.builder.pauserSupplier();
    }

    public void setPauserSupplier(Supplier<Pauser> pauserSupplier) {
        this.builder.pauserSupplier(pauserSupplier);
    }

    public int getIndexCount() {
        return this.builder.indexCount();
    }

    public void setIndexCount(int indexCount) {
        this.builder.indexCount(indexCount);
    }

    public int getIndexSpacing() {
        return this.builder.indexSpacing();
    }

    public void setIndexSpacing(int indexSpacing) {
        this.builder.indexSpacing(indexSpacing);
    }

    public TimeProvider getTimeProvider() {
        return this.builder.timeProvider();
    }

    public void setTimeProvider(TimeProvider timeProvider) {
        this.builder.timeProvider(timeProvider);
    }

    public void setTimeout(long timeout, TimeUnit timeUnit) {
        setTimeout(timeUnit.toMillis(timeout));
    }

    public void setTimeout(long timeout) {
        this.builder.timeoutMS(timeout);
    }

    public long getTimeout() {
        return this.builder.timeoutMS();
    }

    public StoreRecoveryFactory getStoreRecoveryFactory() {
        return this.builder.recoverySupplier();
    }

    public void setStoreRecoveryFactory(StoreRecoveryFactory storeRecoveryFactory) {
        this.builder.recoverySupplier(storeRecoveryFactory);
    }

    public WireType getWireType() {
        return this.builder.wireType();
    }

    public void setWireType(WireType wireType) {
        this.builder.wireType(wireType);
    }

    // *************************************************************************
    //
    // *************************************************************************

    public String[] keys() {
        return KEYS;
    }

    public ChronicleQueue build(String path) throws IOException {
        return new SingleChronicleQueueBuilder(path)
            .buffered(this.isBuffered())
            .blockSize(this.getBlockSize())
            .bufferCapacity(this.getBufferCapacity())
            .rollCycle(this.getRollCycle())
            .epoch(this.getEpoch())
            .pauserSupplier(this.getPauserSupplier())
            .indexCount(this.getIndexCount())
            .indexSpacing(this.getIndexSpacing())
            .wireType(this.getWireType())
            .build();
    }



    public void setProperties(@NotNull final Properties properties, @Nullable final String prefix) {
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            final String name = entry.getKey().toString();
            final String value = entry.getValue().toString();

            if(prefix != null && !prefix.isEmpty()) {
                if (name.startsWith(prefix)) {
                    setProperty(name.substring(prefix.length()), value);
                }
            } else {
                setProperty(name, value);
            }
        }
    }

    public void setProperty(@NotNull final String propName, @NotNull final String propValue) {
        try {
            final PropertyDescriptor property = new PropertyDescriptor(propName, this.getClass());
            final Method method = property.getWriteMethod();
            final Class<?> type = method.getParameterTypes()[0];

            if(type != null && propValue != null && !propValue.isEmpty()) {
                if (type == int.class) {
                    method.invoke(this, Integer.parseInt(propValue));
                } else if (type == long.class) {
                    method.invoke(this, Long.parseLong(propValue));
                } else if (type == boolean.class) {
                    method.invoke(this, Boolean.parseBoolean(propValue));
                } else if (type == String.class) {
                    method.invoke(this, propValue);
                }
            }
        } catch (Exception e) {
        }
    }
}
