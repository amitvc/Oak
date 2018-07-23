/**
 * Copyright 2018 Oath Inc.
 * Licensed under the terms of the Apache 2.0 license.
 * Please see LICENSE file in the project root for terms.
 */

package oak;

public class HeapUsageTest {

    private static final long K = 1024;
    private static final long M = K * K;
    private static int keySize = 10;
    private static int valSize = (int) Math.round(5 * K);

    public static class HeapUsageTestKeySizeCalculator implements SizeCalculator<Integer> {

        public int calculateSize(Integer object) {
            return keySize;
        }
    }

    public static class HeapUsageTestValueSizeCalculator implements SizeCalculator<Integer> {

        public int calculateSize(Integer object) {
            return valSize;
        }
    }

    public static void main(String[] args) throws InterruptedException {

        OakMapBuilder builder = OakMapBuilder
                .getDefaultBuilder()
                .setChunkMaxItems(2048)
                .setChunkBytesPerItem(100)
                .setKeySizeCalculator(new HeapUsageTestKeySizeCalculator())
                .setValueSizeCalculator(new HeapUsageTestValueSizeCalculator());

        int numOfEntries = 360000;

        System.out.println("key size: " + keySize + "B" + ", value size: " + ((double) valSize) / K + "KB");

        Integer key = 0;
        Integer val = 0;



        OakMapOffHeapImpl<Integer, Integer> oak = (OakMapOffHeapImpl<Integer, Integer>) builder.build();

        long heapSize = Runtime.getRuntime().totalMemory(); // Get current size of heap in bytes
        long heapMaxSize = Runtime.getRuntime().maxMemory(); // Get maximum size of heap in bytes
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        System.out.println("\nBefore filling up oak");
        System.out.println("heap size: " + heapSize / M + "MB" + ", heap max size: " + heapMaxSize / M + "MB" + ", heap free size: " + heapFreeSize / M + "MB");
        System.out.println("heap used: " + (heapSize - heapFreeSize) / M + "MB");
        System.out.println("off heap used: " + oak.memoryManager.pool.allocated() / M + "MB");


        for (int i = 0; i < numOfEntries; i++) {
            key = i;
            val = i;
            oak.put(key, val);
        }
        System.out.println("\nAfter filling up oak");
        System.out.println("off heap used: " + oak.memoryManager.pool.allocated() / M + "MB");

        System.gc();

        heapSize = Runtime.getRuntime().totalMemory(); // Get current size of heap in bytes
        heapMaxSize = Runtime.getRuntime().maxMemory(); // Get maximum size of heap in bytes
        heapFreeSize = Runtime.getRuntime().freeMemory();
        System.out.println("heap size: " + heapSize / M + "MB" + ", heap max size: " + heapMaxSize / M + "MB" + ", heap free size: " + heapFreeSize / M + "MB");
        System.out.println("heap used: " + (heapSize - heapFreeSize) / M + "MB");

        for (int i = 0; i < numOfEntries; i++) {
            key = i;
            val = i;
            oak.put(key, val);
        }

        for (Integer i = 0; i < numOfEntries; i++) {
            key = i;
            Integer value = oak.get(key);
            if (value == null) {
                System.out.println("buffer != null i==" + i);
                return;
            }
            if (value != i) {
                System.out.println("buffer.getInt(0) != i i==" + i);
                return;
            }
        }
        System.out.println("\nCheck again");
        System.out.println("off heap used: " + oak.memoryManager.pool.allocated() / M + "MB");
        System.out.println("off heap allocated: " + Integer.MAX_VALUE / M + "MB");
        System.gc();
        heapSize = Runtime.getRuntime().totalMemory(); // Get current size of heap in bytes
        heapMaxSize = Runtime.getRuntime().maxMemory(); // Get maximum size of heap in bytes
        heapFreeSize = Runtime.getRuntime().freeMemory();
        System.out.println("heap size: " + heapSize / M + "MB" + ", heap max size: " + heapMaxSize / M + "MB" + ", heap free size: " + heapFreeSize / M + "MB");
        System.out.println("heap used: " + (heapSize - heapFreeSize) / M + "MB");
        float percent = (100 * (heapSize - heapFreeSize)) / oak.memoryManager.pool.allocated();
        System.out.println("\non/off heap used: " + String.format("%.0f%%", percent));

    }
}
