package pt.ulisboa.tecnico.cnv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

public class SharedInstanceRegistry {
    private static ConcurrentHashMap<String, ServerInstance> instanceMap = new ConcurrentHashMap<>();

    public static void addInstance(String instanceId, ServerInstance instance) {
        instanceMap.put(instanceId, instance);
    }

    public static void removeInstance(String instanceId) {
        instanceMap.remove(instanceId);
    }

    public static ServerInstance getInstance(String instanceId) {
        return instanceMap.get(instanceId);
    }
    
    public static Collection<ServerInstance> getInstances() {
        return instanceMap.values();
    }
}
