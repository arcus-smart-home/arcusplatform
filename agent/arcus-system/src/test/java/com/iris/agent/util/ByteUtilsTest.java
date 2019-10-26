/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.agent.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ByteUtilsTest {

    @Test
    public void testFill() {
        final byte[] testByteArray = new byte[] {1, 2, 3, 4};

        Assert.assertArrayEquals(
            new byte[] {0, 0, 0, 0},
            ByteUtils.fill(testByteArray, 0));
        Assert.assertArrayEquals(
            new byte[] {0, 0, 0, 0},
            testByteArray);

        Assert.assertArrayEquals(
            new byte[] {12, 12, 12, 12},
            ByteUtils.fill(testByteArray, (byte) 12));
        Assert.assertArrayEquals(
            new byte[] {12, 12, 12, 12},
            testByteArray);

        Assert.assertNull(ByteUtils.fill(null, 0));
    }

    @Test
    public void testConcat() {
        Assert.assertArrayEquals(new byte[] {}, ByteUtils.concat(new byte[] {}));
        Assert.assertArrayEquals(new byte[] {0}, ByteUtils.concat(new byte[] {0}));
        Assert.assertArrayEquals(
            new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
            ByteUtils.concat(
                new byte[] {1, 2, 3, 4},
                new byte[] {5, 6, 7, 8}));
    }

    @Test
    public void testSetFlags() {
        Assert.assertEquals(0,
            ByteUtils.setFlags(
                false, false, false, false,
                false, false, false, false));
        Assert.assertEquals(1,
            ByteUtils.setFlags(
                false, false, false, false,
                false, false, false, true));
        Assert.assertEquals(3,
            ByteUtils.setFlags(
                false, false, false, false,
                false, false, true,  true));
        Assert.assertEquals(7,
            ByteUtils.setFlags(
                false, false, false, false,
                false, true,  true,  true));
        Assert.assertEquals(15,
            ByteUtils.setFlags(
                false, false, false, false,
                true,  true,  true,  true));
        Assert.assertEquals(31,
            ByteUtils.setFlags(
                false, false, false, true,
                true,  true,  true,  true));
        Assert.assertEquals(63,
            ByteUtils.setFlags(
                false, false, true,  true,
                true,  true,  true,  true));
        Assert.assertEquals(127,
            ByteUtils.setFlags(
                false, true,  true,  true,
                true,  true,  true,  true));
        Assert.assertEquals(255,
            ByteUtils.setFlags(
                true,  true,  true,  true,
                true,  true,  true,  true));
    }

    @Test
    public void testFrom16BitToInt() {
        Assert.assertEquals(0, ByteUtils.from16BitToInt(new byte[] {0, 0}));
        Assert.assertEquals(256, ByteUtils.from16BitToInt(new byte[] {1, 0}));
        Assert.assertEquals(257, ByteUtils.from16BitToInt(new byte[] {1, 1}));
        Assert.assertEquals(3073, ByteUtils.from16BitToInt(new byte[] {12, 1}));
        Assert.assertEquals(256, ByteUtils.from16BitToInt(new byte[] {0, 1, 0}, 1));
        Assert.assertEquals(257, ByteUtils.from16BitToInt(new byte[] {0, 1, 1, 0}, 1));
    }

    @Test
    public void testInts2BytesPrimitive() {
        Assert.assertArrayEquals(
            new byte[] {},
            ByteUtils.ints2Bytes(new int[] {}));
        Assert.assertArrayEquals(
            new byte[] {1, 2, 3, 4},
            ByteUtils.ints2Bytes(new int[] {1, 2, 3, 4}));
        Assert.assertArrayEquals(
            new byte[] {0, 1, 2, 3},
            ByteUtils.ints2Bytes(new int[] {256, 257, 258, 259}));
    }

    @Test
    public void testTo8Bits() {
        Assert.assertArrayEquals(new byte[] {0}, ByteUtils.to8Bits(0));
    }

    @Test
    public void testTo16Bits() {
        Assert.assertArrayEquals(new byte[] {0, 0}, ByteUtils.to16Bits(0));
        Assert.assertArrayEquals(new byte[] {0, 20}, ByteUtils.to16Bits(20));
        Assert.assertArrayEquals(new byte[] {1, 20}, ByteUtils.to16Bits(276));
    }

    @Test
    public void testTo24Bits() {
        Assert.assertArrayEquals(
            new byte[] {0, 0, 0},
            ByteUtils.to24Bits(0));
        Assert.assertArrayEquals(
            new byte[] {15, 66, 63},
            ByteUtils.to24Bits(999999));
        Assert.assertArrayEquals(
            new byte[] {-102, -55, -1},
            ByteUtils.to24Bits(999999999));
    }

    @Test
    public void testInts2BytesCollection(){
        Assert.assertArrayEquals(new byte[0],
            ByteUtils.ints2Bytes((List) null));

        List<Integer> vals = Arrays.asList(new Integer[] {1, 2, 3, 4});
        Assert.assertArrayEquals(
            new byte[] {1, 2, 3, 4},
            ByteUtils.ints2Bytes(vals));
    }

    @Test
    public void testPrepend() {
        Assert.assertArrayEquals(
            new byte[] {1},
            ByteUtils.prepend(null, (byte) 1));
        Assert.assertArrayEquals(
            new byte[] {10,1,2,3},
            ByteUtils.prepend(new byte[] {1, 2, 3}, (byte) 10));
    }

    @Test
    public void testString2bytes() {
        Assert.assertArrayEquals(
            new byte[] {},
            ByteUtils.string2bytes(null));
        Assert.assertArrayEquals(
            new byte[] {1, 2, 10},
            ByteUtils.string2bytes("01020A"));
    }

    @Test
    public void testTo32Bits() {
        Assert.assertArrayEquals(new byte[] {0, 0, 0, 0}, ByteUtils.to32Bits(0));
        Assert.assertArrayEquals(new byte[] {0, 0, 0, 0}, ByteUtils.to32Bits(0L));
    }

    @Test
    public void testFrom24BitToInt() {
        Assert.assertEquals(0, ByteUtils.from24BitToInt(new byte[] {0, 0, 0}));
        Assert.assertEquals(0, ByteUtils.from24BitToInt(new byte[] {0, 0, 0}, 0));
    }

    @Test
    public void testFrom32BitToInt() {
        Assert.assertEquals(0, ByteUtils.from32BitToInt(new byte[] {0, 0, 0, 0, 1, 1}, 0));
        Assert.assertEquals(0, ByteUtils.from32BitToInt(new byte[] {0, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    public void testByteArray2SpacedString(){
        Assert.assertEquals("", ByteUtils.byteArray2SpacedString(new byte[0]));
        Assert.assertEquals("01 02 03", ByteUtils.byteArray2SpacedString(new byte[]{1, 2, 3}));
    }

    @Test
    public void testByteArray2StringBlock() {
        Assert.assertEquals("", ByteUtils.byteArray2StringBlock(new byte[] {}, 0));
        Assert.assertEquals(
            "01 02 03 04\n",
            ByteUtils.byteArray2StringBlock(4, new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8}, 4));
        Assert.assertEquals(
            "01 02 03 04\n" +
            "05 06 07 08\n",
            ByteUtils.byteArray2StringBlock(new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8}, 4));
        Assert.assertEquals(
            "  01 02 03 04\n",
            ByteUtils.byteArray2StringBlock(4, new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8}, 4, 2));
        Assert.assertEquals(
            "  01 02 03 04\n" +
            "  05 06 07 08\n",
            ByteUtils.byteArray2StringBlock(new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8}, 4, 2));
        Assert.assertEquals(
            "    01 02 03 04\n" +
            "  05 06 07 08\n",
            ByteUtils.byteArray2StringBlock(new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8}, 4, 2, 4));
    }

    @Test
    public void testByteArray2Ints() {
        Assert.assertArrayEquals(
            new int[] {0},
            ByteUtils.byteArray2Ints(new byte[] {}, 268_435_457, 1));
        Assert.assertArrayEquals(
            new int[] {},
            ByteUtils.byteArray2Ints(new byte[] {}, 2_147_254_272, 0));
        Assert.assertArrayEquals(
            new int[] {1},
            ByteUtils.byteArray2Ints(new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1}, 8, 1));
    }

    @Test
    public void testSetBit() {
        Assert.assertEquals((byte) 0b01000001, ByteUtils.setBit((byte) 0b00000001, 6));
        Assert.assertEquals((byte) 0b11110100, ByteUtils.setBit((byte) 0b11110000, 2));
        Assert.assertEquals((byte) 0b00001000, ByteUtils.setBit((byte) 0b00001000, 3));
    }

    @Test
    public void testIsSet(){
        Assert.assertTrue(ByteUtils.isSet(0b00001000, 0b00111000));
        Assert.assertTrue(ByteUtils.isSet(0b10000000, 0b11100001));

        Assert.assertFalse(ByteUtils.isSet(0b00000001, 0b00000000));
        Assert.assertFalse(ByteUtils.isSet(0b00000001, 0b11111110));
    }

    @Test
    public void testToByteArray() {
        Assert.assertArrayEquals(new byte[] {0}, ByteUtils.toByteArray((byte) 0));
        Assert.assertArrayEquals(new byte[] {10}, ByteUtils.toByteArray((byte) 10));
    }

    @Test
    public void testClone() {
        Assert.assertNull(ByteUtils.clone(null));
        Assert.assertArrayEquals(
            new byte[] {1, 2, 3, 4},
            ByteUtils.clone(new byte[] {1, 2, 3, 4}));
    }
}
