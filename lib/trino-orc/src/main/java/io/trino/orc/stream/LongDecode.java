/*
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
package io.trino.orc.stream;

import io.airlift.slice.SliceOutput;
import io.trino.orc.OrcCorruptionException;

import java.io.IOException;

import static io.trino.orc.stream.LongDecode.FixedBitSizes.FIFTY_SIX;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.FORTY;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.FORTY_EIGHT;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.ONE;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.THIRTY;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.THIRTY_TWO;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.TWENTY_EIGHT;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.TWENTY_FOUR;
import static io.trino.orc.stream.LongDecode.FixedBitSizes.TWENTY_SIX;

// This is based on the Apache Hive ORC code
public final class LongDecode
{
    private LongDecode() {}

    enum FixedBitSizes
    {
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, ELEVEN, TWELVE,
        THIRTEEN, FOURTEEN, FIFTEEN, SIXTEEN, SEVENTEEN, EIGHTEEN, NINETEEN,
        TWENTY, TWENTY_ONE, TWENTY_TWO, TWENTY_THREE, TWENTY_FOUR, TWENTY_SIX,
        TWENTY_EIGHT, THIRTY, THIRTY_TWO, FORTY, FORTY_EIGHT, FIFTY_SIX, SIXTY_FOUR
    }

    /**
     * Decodes the ordinal fixed bit value to actual fixed bit width value.
     */
    public static int decodeBitWidth(int n)
    {
        if (n >= ONE.ordinal() && n <= TWENTY_FOUR.ordinal()) {
            return n + 1;
        }
        if (n == TWENTY_SIX.ordinal()) {
            return 26;
        }
        if (n == TWENTY_EIGHT.ordinal()) {
            return 28;
        }
        if (n == THIRTY.ordinal()) {
            return 30;
        }
        if (n == THIRTY_TWO.ordinal()) {
            return 32;
        }
        if (n == FORTY.ordinal()) {
            return 40;
        }
        if (n == FORTY_EIGHT.ordinal()) {
            return 48;
        }
        if (n == FIFTY_SIX.ordinal()) {
            return 56;
        }
        return 64;
    }

    /**
     * Gets the closest supported fixed bit width for the specified bit width.
     */
    public static int getClosestFixedBits(int width)
    {
        if (width == 0) {
            return 1;
        }

        if (width >= 1 && width <= 24) {
            return width;
        }
        if (width > 24 && width <= 26) {
            return 26;
        }
        if (width > 26 && width <= 28) {
            return 28;
        }
        if (width > 28 && width <= 30) {
            return 30;
        }
        if (width > 30 && width <= 32) {
            return 32;
        }
        if (width > 32 && width <= 40) {
            return 40;
        }
        if (width > 40 && width <= 48) {
            return 48;
        }
        if (width > 48 && width <= 56) {
            return 56;
        }
        return 64;
    }

    public static long readSignedVInt(OrcInputStream inputStream)
            throws IOException
    {
        long result = readUnsignedVInt(inputStream);
        return zigzagDecode(result);
    }

    private static long readUnsignedVInt(OrcInputStream inputStream)
            throws IOException
    {
        long result = 0;
        int offset = 0;
        long b;
        do {
            b = inputStream.read();
            if (b == -1) {
                throw new OrcCorruptionException(inputStream.getOrcDataSourceId(), "EOF while reading unsigned vint");
            }
            result |= (b & 0b0111_1111) << offset;
            offset += 7;
        }
        while ((b & 0b1000_0000) != 0);
        return result;
    }

    public static long readVInt(boolean signed, OrcInputStream inputStream)
            throws IOException
    {
        if (signed) {
            return readSignedVInt(inputStream);
        }
        return readUnsignedVInt(inputStream);
    }

    public static long zigzagDecode(long value)
    {
        return (value >>> 1) ^ -(value & 1);
    }

    public static void writeVLong(SliceOutput buffer, long value, boolean signed)
    {
        if (signed) {
            value = zigzagEncode(value);
        }
        writeVLongUnsigned(buffer, value);
    }

    private static void writeVLongUnsigned(SliceOutput output, long value)
    {
        while (true) {
            // if there are less than 7 bits left, we are done
            if ((value & ~0b111_1111) == 0) {
                output.write((byte) value);
                return;
            }
            output.write((byte) (0x80 | (value & 0x7f)));
            value >>>= 7;
        }
    }

    private static long zigzagEncode(long value)
    {
        return (value << 1) ^ (value >> 63);
    }
}
