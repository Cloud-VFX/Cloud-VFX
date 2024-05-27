package pt.ulisboa.tecnico.cnv;

public class ServerInstance {
    private String instanceId;
    private String address;

    public ServerInstance(String instanceId, String address) {
        this.instanceId = instanceId;
        this.address = address;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getAddress() {
        return address;
    }
}
