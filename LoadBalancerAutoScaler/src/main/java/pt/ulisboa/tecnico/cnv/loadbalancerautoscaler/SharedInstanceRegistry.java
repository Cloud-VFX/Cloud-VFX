package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Collection;

public class SharedInstanceRegistry {
    private static ConcurrentHashMap<String, ServerInstance> instanceMap = new ConcurrentHashMap<>();
    private static PriorityBlockingQueue<ServerInstance> instanceQueue = new PriorityBlockingQueue<>(
            10,
            (a, b) -> Double.compare(a.getTotalComplexity(), b.getTotalComplexity()));

    public enum UpdateComplexityType {
        ADD, REMOVE
    }

    public static void addInstance(String instanceId, ServerInstance instance) {
        instanceMap.put(instanceId, instance);
        instanceQueue.add(instance);
    }

    public static void removeInstance(String instanceId) {
        ServerInstance instance = instanceMap.remove(instanceId);
        if (instance != null) {
            instanceQueue.remove(instance);
        }
    }

    public static ServerInstance getInstance(String instanceId) {
        return instanceMap.get(instanceId);
    }

    public static synchronized void updateInstanceComplexity(String instanceId, double newComplexity,
            UpdateComplexityType type) {
        ServerInstance instance = instanceMap.get(instanceId);
        if (instance == null) {
            return;
        }

        instanceQueue.remove(instance);
        if (type == UpdateComplexityType.ADD) {
            instance.addComplexity(newComplexity);
        } else {
            instance.removeComplexity(newComplexity);
        }
        instanceQueue.add(instance);

    }

    public static Collection<ServerInstance> getInstances() {
        return instanceMap.values();
    }

    public static ServerInstance getInstanceWithLessComplexity() {
        return instanceQueue.peek();
    }
}
