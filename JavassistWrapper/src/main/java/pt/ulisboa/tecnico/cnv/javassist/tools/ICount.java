package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.concurrent.ConcurrentHashMap;

public class ICount extends CodeDumper {

    public static class Metrics {
        public long nblocks = 0;
        public long nmethods = 0;
        public long ninsts = 0;
    }

    private static final ConcurrentHashMap<Long, Metrics> metricsMap = new ConcurrentHashMap<>();

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        long threadId = Thread.currentThread().threadId();
        metricsMap.computeIfAbsent(threadId, k -> new Metrics()).nblocks++;
        metricsMap.get(threadId).ninsts += length;
    }

    public static void incBehavior(String name) {
        long threadId = Thread.currentThread().threadId();
        metricsMap.computeIfAbsent(threadId, k -> new Metrics()).nmethods++;
    }

    public static void printStatistics(String name) {
        long threadId = Thread.currentThread().threadId();
        Metrics m = metricsMap.get(threadId);
        if (m != null) {
            System.out.println(String.format("[%s][%s][Thread %d] Number of executed methods: %s",
                    ICount.class.getSimpleName(), name, threadId, m.nmethods));
            System.out.println(String.format("[%s][%s][Thread %d] Number of executed basic blocks: %s",
                    ICount.class.getSimpleName(), name, threadId, m.nblocks));
            System.out.println(String.format("[%s][%s][Thread %d] Number of executed instructions: %s",
                    ICount.class.getSimpleName(), name, threadId, m.ninsts));
        }
    }

    public static void clearStatistics() {
        long threadId = Thread.currentThread().threadId();
        metricsMap.remove(threadId);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ICount.class.getName(), behavior.getLongName()));
        behavior.insertAfter(String.format("%s.printStatistics((\"%s\"));", ICount.class.getName(), behavior.getLongName()));
        behavior.insertAfter(String.format("%s.clearStatistics();", ICount.class.getName()));
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ICount.class.getName(),
                block.getPosition(), block.getLength()));
    }

}
