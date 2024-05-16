package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.concurrent.ConcurrentHashMap;

public class RAMUsageTracker extends CodeDumper {

    public static class Metrics {
        public long maxMemoryUsed = 0;
    }

    private static final ConcurrentHashMap<Long, Metrics> metricsMap = new ConcurrentHashMap<>();

    public RAMUsageTracker(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void updateMemoryUsage() {
        long getId = Thread.currentThread().getId();
        Metrics metrics = metricsMap.computeIfAbsent(getId, k -> new Metrics());
        long currentMemoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        if (currentMemoryUsed > metrics.maxMemoryUsed) {
            metrics.maxMemoryUsed = currentMemoryUsed;
        }
    }

    public static void printStatistics() {
        long getId = Thread.currentThread().getId();
        Metrics m = metricsMap.get(getId);
        if (m != null) {
            System.out.println(String.format("[%s][Thread %d] Maximum memory used: %d bytes",
                    RAMUsageTracker.class.getSimpleName(), getId, m.maxMemoryUsed));
        }
    }

    public static void clearStatistics() {
        long getId = Thread.currentThread().getId();
        metricsMap.remove(getId);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        String template = String.format("%s.updateMemoryUsage();", RAMUsageTracker.class.getName());
        behavior.insertBefore(template);
        behavior.insertAfter(template);
        behavior.insertAfter(String.format("%s.printStatistics();", RAMUsageTracker.class.getName()));
        behavior.insertAfter(String.format("%s.clearStatistics();", RAMUsageTracker.class.getName()));
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.updateMemoryUsage();", RAMUsageTracker.class.getName()));
    }

}
