package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class InstanceSelector {
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /*
     * Implementation of a round-robin instance selection algorithm
     */
    public ServerInstance getNextInstance() {
        Collection<ServerInstance> instances = SharedInstanceRegistry.getInstances();
        if (instances.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndIncrement() % instances.size();
        return instances.toArray(new ServerInstance[0])[index];
    }

    public ServerInstance getInstanceWithLessComplexity() {
        return SharedInstanceRegistry.getInstanceWithLessComplexity();
    }
}
