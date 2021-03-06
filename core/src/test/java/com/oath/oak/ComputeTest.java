/*
 * Copyright 2018 Oath Inc.
 * Licensed under the terms of the Apache 2.0 license.
 * Please see LICENSE file in the project root for terms.
 */

package com.oath.oak;

import com.oath.oak.common.OakCommonBuildersFactory;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ComputeTest {

    private static int NUM_THREADS = 16;

    private static OakMap<ByteBuffer, ByteBuffer> oak;
    private static final long K = 1024;

    private static int keySize = 10;
    private static int valSize = Math.round(5 * K);
    private static int numOfEntries;


    static private ArrayList<Thread> threads = new ArrayList<>(NUM_THREADS);
    static private CountDownLatch latch = new CountDownLatch(1);

    private static Consumer<OakWriteBuffer> computer = oakWBuffer -> {
        if (oakWBuffer.getInt(0) == oakWBuffer.getInt(Integer.BYTES * keySize)) {
            return;
        }
        int index = 0;
        int[] arr = new int[keySize];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < keySize; j++) {
                arr[j] = oakWBuffer.getInt(index);
                index += Integer.BYTES;
            }
            for (int j = 0; j < keySize; j++) {
                oakWBuffer.putInt(index, arr[j]);
                index += Integer.BYTES;
            }
        }
    };

    static class RunThreads implements Runnable {
        CountDownLatch latch;

        RunThreads(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ByteBuffer myKey = ByteBuffer.allocate(keySize * Integer.BYTES);
            ByteBuffer myVal = ByteBuffer.allocate(valSize * Integer.BYTES);

            Random r = new Random();

            for (int i = 0; i < 1000000; i++) {
                int k = r.nextInt(numOfEntries);
                int o = r.nextInt(2);
                myKey.putInt(0, k);
                myVal.putInt(0, k);
                if (o % 2 == 0) {
                    oak.zc().computeIfPresent(myKey, computer);
                } else {
                    oak.zc().putIfAbsent(myKey, myVal);
                }

            }

        }
    }

    @Test
    public void testMain() throws InterruptedException {
        ByteBuffer minKey = ByteBuffer.allocate(keySize * Integer.BYTES);
        minKey.position(0);
        for (int i = 0; i < keySize; i++) {
            minKey.putInt(4 * i, Integer.MIN_VALUE);
        }
        minKey.position(0);

        OakMapBuilder<ByteBuffer, ByteBuffer> builder = OakCommonBuildersFactory.getDefaultIntBufferBuilder(keySize, valSize)
            .setChunkMaxItems(2048);

        oak = builder.build();


        numOfEntries = 100;


        for (int i = 0; i < NUM_THREADS; i++) {
            threads.add(new Thread(new RunThreads(latch)));
        }

        for (int i = 0; i < (int) Math.round(numOfEntries * 0.5); i++) {
            ByteBuffer key = ByteBuffer.allocate(keySize * Integer.BYTES);
            ByteBuffer val = ByteBuffer.allocate(valSize * Integer.BYTES);
            key.putInt(0, i);
            val.putInt(0, i);
            oak.zc().putIfAbsent(key, val);
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            threads.get(i).start();
        }

        latch.countDown();

        for (int i = 0; i < NUM_THREADS; i++) {
            threads.get(i).join();
        }

        for (int i = 0; i < numOfEntries; i++) {
            ByteBuffer key = ByteBuffer.allocate(keySize * Integer.BYTES);
            key.putInt(0, i);
            ByteBuffer val = oak.get(key);
            if (val == null) {
                continue;
            }
            assertEquals(i, val.getInt(0));
            int forty = val.getInt((keySize - 1) * Integer.BYTES);
            assertTrue(forty == i || forty == 0);
        }

        oak.close();
    }
}
