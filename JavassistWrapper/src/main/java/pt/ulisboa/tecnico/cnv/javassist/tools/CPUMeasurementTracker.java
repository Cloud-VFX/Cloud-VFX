package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class CPUMeasurementTracker extends CodeDumper {

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static final ConcurrentHashMap<Long, Long> startTimeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> totalCPUTimeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Integer> countMap = new ConcurrentHashMap<>();

    public CPUMeasurementTracker(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void startTracking() {
        long threadId = Thread.currentThread().getId();
        startTimeMap.put(threadId, threadMXBean.getCurrentThreadCpuTime());
    }

    public static void stopTracking() {
        long threadId = Thread.currentThread().getId();
        long endTime = threadMXBean.getCurrentThreadCpuTime();
        long startTime = startTimeMap.getOrDefault(threadId, endTime);
        long duration = endTime - startTime;

        totalCPUTimeMap.compute(threadId, (k, v) -> (v == null) ? duration : v + duration);
        countMap.compute(threadId, (k, v) -> (v == null) ? 1 : v + 1);
    }

    public static void printStatistics() {
        long threadId = Thread.currentThread().getId();
        Long totalCpuTime = totalCPUTimeMap.getOrDefault(threadId, 0L);
        Integer count = countMap.getOrDefault(threadId, 0);

        if (count > 0) {
            double averageCpuTime = totalCpuTime / (double) count;
            System.out.println(String.format("[%s][Thread %d] Average CPU time used: %.2f ns",
                    CPUMeasurementTracker.class.getSimpleName(), threadId, averageCpuTime));
        }
    }

    public static void clearStatistics() {
        long threadId = Thread.currentThread().getId();
        totalCPUTimeMap.remove(threadId);
        countMap.remove(threadId);
        startTimeMap.remove(threadId);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertBefore(CPUMeasurementTracker.class.getName() + ".startTracking();");
        behavior.insertAfter(CPUMeasurementTracker.class.getName() + ".stopTracking();", true);
        behavior.insertAfter(CPUMeasurementTracker.class.getName() + ".printStatistics();", true);
        behavior.insertAfter(CPUMeasurementTracker.class.getName() + ".clearStatistics();", true);
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, CPUMeasurementTracker.class.getName() + ".startTracking();");
        block.behavior.insertAt(block.line + 1, CPUMeasurementTracker.class.getName() + ".stopTracking();");
    }

}
