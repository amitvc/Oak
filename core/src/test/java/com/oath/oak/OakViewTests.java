package com.oath.oak;

import com.oath.oak.common.OakCommonBuildersFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OakViewTests {

    private OakMap<String, String> oak;
    private static final int ELEMENTS = 1000;

    @Before
    public void init() {
        OakMapBuilder<String, String> builder = OakCommonBuildersFactory.getDefaultStringBuilder()
            .setChunkMaxItems(100);

        oak = builder.build();

        for (int i = 0; i < ELEMENTS; i++) {
            String key = String.valueOf(i);
            String val = String.valueOf(i);
            oak.zc().put(key, val);
        }
    }

    @After
    public void tearDown() {
        oak.close();
    }

    @Test
    public void uniTestOakDetachedBufferBuffer() {
        String testVal = String.valueOf(123);
        ByteBuffer testValBB = ByteBuffer.allocate(Integer.BYTES + testVal.length() * Character.BYTES);
        testValBB.putInt(0, testVal.length());
        for (int i = 0; i < testVal.length(); ++i) {
            testValBB.putChar(Integer.BYTES + i * Character.BYTES, testVal.charAt(i));
        }

        OakDetachedBuffer valBuffer = oak.zc().get(testVal);
        String transformed = valBuffer.transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
        assertEquals(testVal, transformed);

        assertEquals(testValBB.capacity(), valBuffer.capacity());

        assertEquals(testValBB.getInt(0), valBuffer.getInt(0));
        for (int i = 0; i < testVal.length(); i++) {
            int pos = Integer.BYTES + i * Character.BYTES;
            assertEquals(testValBB.getChar(pos), valBuffer.getChar(pos));
        }

        for (int i = 0; i < testVal.length(); ++i) {
            int pos = Integer.BYTES + i * Character.BYTES;
            assertEquals(testVal.charAt(i), valBuffer.getChar(pos));
        }
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testOakRBufferConcurrency() {
        String testVal = "987";
        OakDetachedBuffer valBuffer = oak.zc().get(testVal);
        oak.zc().remove(testVal);
        valBuffer.get(0);
    }

    @Test
    public void testBufferViewAPIs() {
        String[] values = new String[ELEMENTS];
        for (int i = 0; i < ELEMENTS; i++) {
            values[i] = String.valueOf(i);
        }
        Arrays.sort(values);

        Iterator<OakDetachedBuffer> keyIterator = oak.zc().keySet().iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            OakDetachedBuffer keyBB = keyIterator.next();
            String key = keyBB.transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
            assertEquals(values[i], key);
        }

        Iterator<OakDetachedBuffer> valueIterator = oak.zc().values().iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            OakDetachedBuffer valueBB = valueIterator.next();
            String value = valueBB.transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
            assertEquals(values[i], value);
        }

        Iterator<Map.Entry<OakDetachedBuffer, OakDetachedBuffer>> entryIterator = oak.zc().entrySet().iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            Map.Entry<OakDetachedBuffer, OakDetachedBuffer> entryBB = entryIterator.next();
            String value = entryBB.getValue().transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
            assertEquals(values[i], value);
        }
    }

    @Test
    public void testStreamAPIs() {
        String[] values = new String[ELEMENTS];
        for (int i = 0; i < ELEMENTS; i++) {
            values[i] = String.valueOf(i);
        }
        Arrays.sort(values);

        Iterator<OakDetachedBuffer> keyStreamIterator = oak.zc().keyStreamSet().iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            OakDetachedBuffer keyBB = keyStreamIterator.next();
            String key = keyBB.transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
            assertEquals(values[i], key);
        }

        Iterator<OakDetachedBuffer> valueStreamIterator = oak.zc().valuesStream().iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            OakDetachedBuffer valueBB = valueStreamIterator.next();
            String value = valueBB.transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
            assertEquals(values[i], value);
        }

        Iterator<Map.Entry<OakDetachedBuffer, OakDetachedBuffer>> entryStreamIterator
                = oak.zc().entryStreamSet().iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            Map.Entry<OakDetachedBuffer, OakDetachedBuffer> entryBB = entryStreamIterator.next();
            String value = entryBB.getValue().transform(OakCommonBuildersFactory.defaultStringSerializer::deserialize);
            assertEquals(values[i], value);
        }
    }

    @Test
    public void testTransformViewAPIs() {
        Function<Map.Entry<OakDetachedBuffer, OakDetachedBuffer>, Integer> transformer = (entry) -> {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            int size = entry.getValue().getInt(0);
            StringBuilder object = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                char c = entry.getValue().getChar(Integer.BYTES + i * Character.BYTES);
                object.append(c);
            }
            return Integer.valueOf(object.toString());
        };


        String[] values = new String[ELEMENTS];
        for (int i = 0; i < ELEMENTS; i++) {
            values[i] = String.valueOf(i);
        }
        Arrays.sort(values);

        Iterator<Integer> entryIterator = oak.zc().entrySet().stream().map(transformer).iterator();
        for (int i = 0; i < ELEMENTS; i++) {
            Integer entryT = entryIterator.next();
            assertEquals(Integer.valueOf(values[i]), entryT);
        }
    }
}
