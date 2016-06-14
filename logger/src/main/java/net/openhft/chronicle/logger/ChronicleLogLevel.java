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

import java.io.IOException;

import net.openhft.chronicle.bytes.ByteStringAppender;
import net.openhft.chronicle.bytes.RandomDataInput;
import net.openhft.chronicle.bytes.RandomDataOutput;

public enum ChronicleLogLevel {
    ERROR(50,"ERROR"),
    WARN (40,"WARN" ),
    INFO (30,"INFO" ),
    DEBUG(20,"DEBUG"),
    TRACE(10,"TRACE");

    /**
     * Array is not cached in Java enum internals, make the single copy to prevent
     * garbage creation
     */
    private static final ChronicleLogLevel[] VALUES = values();

    private final int levelInt;
    private final String levelStr;

    ChronicleLogLevel(int levelInt, String levelStr) {
        this.levelInt = levelInt;
        this.levelStr = levelStr;
    }

    public boolean isHigherOrEqualTo(final ChronicleLogLevel presumablyLowerLevel) {
        return levelInt >= presumablyLowerLevel.levelInt;
    }

    public void printTo(final ByteStringAppender appender) throws IOException {
        appender.append(levelStr);
    }

    public void writeTo(final RandomDataOutput out) {
        out.writeByte(0, ordinal());
    }

    @Override
    public String toString() {
        return levelStr;
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static ChronicleLogLevel readBinary(final RandomDataInput in) {
        return VALUES[in.readByte(0)];
    }

    public static ChronicleLogLevel fromStringLevel(final CharSequence levelStr) {
        if (levelStr != null) {
            for (int i = VALUES.length; i >= 0; i--) {
                if (ChronicleLog.fastEqualsIgnoreCase(VALUES[i].levelStr, levelStr)) {
                    return VALUES[i];
                }
            }
        }

        throw new IllegalArgumentException(levelStr + " not a valid level value");
    }
}