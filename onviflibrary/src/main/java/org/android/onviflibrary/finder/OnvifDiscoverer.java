package org.android.onviflibrary.finder;

public class OnvifDiscoverer {

    private String host;
    private String uuid;
    private String address;

    public OnvifDiscoverer(String host,String uuid, String address) {
        this.uuid = uuid;
        this.address = address;
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "OnvifDiscoverer{" +
                "host='" + host + '\'' +
                ", uuid='" + uuid + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
