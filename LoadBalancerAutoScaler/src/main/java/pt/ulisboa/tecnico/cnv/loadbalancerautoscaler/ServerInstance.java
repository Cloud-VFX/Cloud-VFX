package pt.ulisboa.tecnico.cnv.loadbalancerautoscaler;

public class ServerInstance implements Comparable<ServerInstance> {
    private String instanceId;
    private String address;
    private int numRunningRequests;
    private double totalComplexity;
    private boolean markedForTermination;

    /**
     * Constructor for ServerInstance with instance ID and address initialization.
     */
    public ServerInstance(String instanceId, String address) {
        this.instanceId = instanceId;
        this.address = address;
        this.totalComplexity = 0.0f;
        this.numRunningRequests = 0;
        this.markedForTermination = false;
    }

    // Getters for instance ID and address
    public String getInstanceId() {
        return instanceId;
    }

    public String getAddress() {
        return address;
    }

    public boolean isMarkedForTermination() {
        return markedForTermination;
    }

    public void markForTermination() {
        this.markedForTermination = true;
    }

    public void increaseNumRunningRequests() {
        this.numRunningRequests += 1;
    }

    public void decreaseNumRunningRequests() {
        this.numRunningRequests -= 1;
    }

    public int getNumRunningRequests() {
        return numRunningRequests;
    }

    /**
     * Adds complexity to the total complexity count.
     * 
     * @param complexity amount to add
     */
    public void addComplexity(double complexity) {
        this.totalComplexity += complexity;
    }

    /**
     * Removes complexity from the total complexity count.
     * 
     * @param complexity amount to remove
     */
    public void removeComplexity(double complexity) {
        this.totalComplexity -= complexity;
    }

    /**
     * Gets the current total complexity.
     * 
     * @return total complexity
     */
    public double getTotalComplexity() {
        return totalComplexity;
    }

    /**
     * Converts server instance details to a string format.
     * 
     * @return String representation of the server instance
     */
    @Override
    public String toString() {
        return "ServerInstance{" +
                "instanceId='" + instanceId + '\'' +
                ", address='" + address + '\'' +
                ", totalComplexity=" + totalComplexity +
                ", numRequests=" + numRunningRequests +
                '}';
    }

    @Override
    public int compareTo(ServerInstance o) {
        return Double.compare(this.totalComplexity, o.totalComplexity);
    }
}
