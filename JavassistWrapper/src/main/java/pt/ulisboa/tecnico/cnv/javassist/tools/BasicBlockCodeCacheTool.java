package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicBlockCodeCacheTool extends AbstractJavassistTool {
    private static final int CACHE_SIZE = 128;  // Capacity of the basic block code cache
    private Map<Integer, BasicBlock> blockCache = new ConcurrentHashMap<>();
    private Map<BasicBlock, AtomicInteger> executionCount = new ConcurrentHashMap<>();
    private AtomicInteger hits = new AtomicInteger(0);
    private AtomicInteger misses = new AtomicInteger(0);

    public BasicBlockCodeCacheTool(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        // Increment the execution count for the block
        executionCount.computeIfAbsent(block, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Manage cache and print performance within the transformation
        String formatted = manageCacheAndPrintPerformance(block);

        block.behavior.insertAt(block.line, String.format("System.out.println(\"%s\");", formatted));

        super.transform(block);
    }

    private String manageCacheAndPrintPerformance(BasicBlock block) {
        boolean isHit = blockCache.containsKey(block.position);
        if (isHit) {
            // Cache hit, refresh block in cache
            blockCache.put(block.position, block);
            hits.incrementAndGet();
        } else {
            // Cache miss, add to cache
            if (blockCache.size() >= CACHE_SIZE) {
                // Cache is full, remove least recently used (LRU) entry
                Integer lruKey = blockCache.keySet().iterator().next();
                blockCache.remove(lruKey);
            }
            blockCache.put(block.position, block);
            misses.incrementAndGet();
        }

        // Print cache performance after updating
        return printCachePerformance(block, isHit);
    }

    private String printCachePerformance(BasicBlock block, boolean isHit) {
        String status = isHit ? "Hit" : "Miss";

        String formatted = String.format("Cache %s: Block at position %d in method %s, Hits: %d, Misses: %d",
                                         status, block.position, block.behavior.getName(), hits.get(), misses.get());

        return formatted;
    }
}
