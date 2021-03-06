package com.oath.oak;

import com.oath.oak.synchrobench.MyBuffer;
import com.oath.oak.synchrobench.contention.benchmark.Parameters;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class OakMyBufferTest {

    OakMyBufferMap<MyBuffer, MyBuffer> oakBench;

    static final int size = 1000;
    static final int range = 2048;

    final private static ThreadLocal<Random> s_random = new ThreadLocal<Random>() {
        @Override
        protected synchronized Random initialValue() {
            return new Random();
        }
    };

    @Before
    public void init() {
        oakBench = new OakMyBufferMap<>();
    }

    @Test
    public void testPut() {
        for (long i = size; i > 0; ) {
            Integer v = s_random.get().nextInt(range);
            MyBuffer key = new MyBuffer(Parameters.keySize);
            key.buffer.putInt(0, v);
            MyBuffer val = new MyBuffer(Parameters.valSize);
            val.buffer.putInt(0, v);
            if (oakBench.putIfAbsentOak(key, val)) {
                i--;
            }
        }
    }

    @Test
    public void testPutMinimal() {
        MyBuffer key = new MyBuffer(Parameters.keySize);
        key.buffer.putInt(0, Integer.MIN_VALUE);
        MyBuffer val = new MyBuffer(Parameters.valSize);
        val.buffer.putInt(0, Integer.MIN_VALUE);
        boolean success = oakBench.putIfAbsentOak(key, val);
        assertTrue(success);
    }
}
