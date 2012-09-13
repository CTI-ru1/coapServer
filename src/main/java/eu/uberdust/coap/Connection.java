package eu.uberdust.coap;

import ch.ethz.inf.vs.californium.coap.Request;

import java.net.DatagramPacket;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 9/10/12
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class Connection {
    private Request request;
    private DatagramPacket packet;
    private String capability;

    @Override
    public String toString() {
        return "Connection{" +
                "request=" + request +
                ", packet=" + packet +
                ", capability='" + capability + '\'' +
                ", node='" + node + '\'' +
                '}';
    }

    public Request getRequest() {
        return request;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public String getCapability() {
        return capability;
    }

    public String getNode() {
        return node;
    }

    private String node;

    public void setRequest(Request request) {
        this.request = request;
    }


    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public void setNode(String node) {
        this.node = node;
    }
}
