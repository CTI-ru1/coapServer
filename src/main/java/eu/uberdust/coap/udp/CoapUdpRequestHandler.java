package eu.uberdust.coap.udp;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.coap.CoapServer;
import eu.uberdust.coap.NodeHandler;
import eu.uberdust.coap.PropertyReader;
import eu.uberdust.coap.UberdustNodeHandler;
import eu.uberdust.communication.websocket.readings.WSReadingsClient;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class CoapUdpRequestHandler implements Runnable, Observer {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpRequestHandler.class);

    private transient final DatagramPacket packet;
    private String node;
    private String capability;
    private Request udpRequest;

    /**
     * Constructor.
     *
     * @param packet The UDP packet of the request.
     */
    public CoapUdpRequestHandler(final DatagramPacket packet) {
        this.packet = packet;
    }

    @Override
    public void run() {
        final byte[] inData = cleanupData(packet.getData());
        udpRequest = (Request) Request.fromByteArray(inData);

        List<Option> udpOptions = udpRequest.getOptions();
        for (Option udpOption : udpOptions) {
            LOGGER.info("udp-option: " + udpOption.getName());
        }


        String requestURI = udpRequest.getUriPath();
        if (udpRequest.getType() == Message.messageType.CON) {
//            sendAck(udpRequest.getMID(), packet.getSocketAddress());
        }

        LOGGER.info(requestURI);
        if (requestURI.equals("/.well-known/core")) {
            LOGGER.info("sending response");


            Response response = createResponse(udpRequest);
            response.setContentType(MediaTypeRegistry.APPLICATION_LINK_FORMAT);

            LOGGER.info(response.getUriPath());
            LOGGER.info(response.getContentType());
            LOGGER.info(response.getMID());


            StringBuilder responsePayload = new StringBuilder("<.well-known/core>");
            Map<String, List<String>> nodes = NodeHandler.getInstance().getNodes();
            for (String s : nodes.keySet()) {
                for (String s1 : nodes.get(s)) {
                    responsePayload.append(",<").append(s).append("/").append(s1).append(">");
                }
            }
            response.setPayload(responsePayload.toString());

            byte[] buf = new byte[1024];
//            LOGGER.info(Converter.getInstance().payloadToString(udpRequest.toByteArray()));
//            LOGGER.info(Converter.getInstance().payloadToString(response.toByteArray()));
            final DatagramPacket replyPacket = new DatagramPacket(buf, 1024);
            replyPacket.setData(response.toByteArray());
            replyPacket.setSocketAddress(packet.getSocketAddress());
            LOGGER.info(packet.getSocketAddress());
            CoapServer.getInstance().sendReply(replyPacket);
            LOGGER.info("done");

        } else {

            LOGGER.info("should go here");
            try {
                String[] uriparts = requestURI.split("/");
                if (uriparts.length == 3) {
                    node = uriparts[1];
                    capability = uriparts[2];
                    URL uberdustReading = new URL(PropertyReader.getInstance().getProperties().get("uberdustURL") + "/node/" + node + "/capability/" + capability + "/latestreading");
                    uberdustReading.openConnection();
                    final InputStream input = uberdustReading.openStream();
                    int data = input.read();
                    StringBuilder sb = new StringBuilder();
                    while (data != -1) {
                        sb.append((char) data);
                        data = input.read();
                    }
                    input.close();
                    String[] parts = sb.toString().split("\t");
                    LOGGER.info("Should respond : " + parts[1]);


                    Response response = createResponse(udpRequest);
                    response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
                    response.setPayload(parts[1]);

                    List<Option> options = udpRequest.getOptions();



                    if (udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
                        UberdustNodeHandler.getInstance().observe(node, capability, this);
                        response.setToken(udpRequest.getToken());
                    }


                    byte[] buf = new byte[1024];
                    final DatagramPacket replyPacket = new DatagramPacket(buf, 1024);
                    replyPacket.setData(response.toByteArray());
                    replyPacket.setSocketAddress(packet.getSocketAddress());
                    LOGGER.info(packet.getSocketAddress());
                    CoapServer.getInstance().sendReply(replyPacket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAck(int mid, SocketAddress socketAddress) {
        final Message ack = new Message(Message.messageType.ACK, 0);
        ack.setMID(mid);


        byte[] buf = new byte[1024];
        final DatagramPacket replyPacket = new DatagramPacket(buf, 1024);
        replyPacket.setData(ack.toByteArray());
        replyPacket.setSocketAddress(socketAddress);
        LOGGER.info(packet.getSocketAddress());

        LOGGER.info("EmptyACK=" + ack.isEmptyACK());
        CoapServer.getInstance().sendReply(replyPacket);

    }

    private Response createResponse(Request udpRequest) {
        Response response = new Response();


        response.setURI(udpRequest.getUriPath());
        response.setMID(udpRequest.getMID());
        List<Option> options = udpRequest.getOptions();
        for (Option option : options) {
            LOGGER.info(option.getName());
            response.addOption(option);
        }
        response.setRequest(udpRequest);
        response.setType(udpRequest.getType());
        response.setCode(udpRequest.getCode());

        return response;
    }

    /**
     * Clear tailing zeros from the udpPacket.
     *
     * @param data the incoming packet.
     * @return the data from the udp packet without tailing zeros.
     */
    private byte[] cleanupData(final byte[] data) {
        int myLen = data.length;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] == 0) {
                myLen = i;
            }
        }
        final byte[] adata = new byte[myLen];
        System.arraycopy(data, 0, adata, 0, myLen);
        LOGGER.info(adata.length);
        return adata;
    }

    @Override
    public void update(Observable o, Object arg) {
        if ((node == null) || (capability == null)) {
            return;
        }
        if (!(o instanceof WSReadingsClient)) {
            return;
        }
        if (!(arg instanceof eu.uberdust.communication.protobuf.Message.NodeReadings)) {
            return;
        }
        eu.uberdust.communication.protobuf.Message.NodeReadings readings = (eu.uberdust.communication.protobuf.Message.NodeReadings) arg;
        for (eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading : readings.getReadingList()) {
            if (reading.hasDoubleReading()) {
                notifyFor(String.valueOf(reading.getDoubleReading()));
            } else if (reading.hasStringReading()) {
                notifyFor(reading.getStringReading());
            }
        }
    }

    private void notifyFor(String stringReading) {

        Response response = createResponse(udpRequest);
        response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
        response.setPayload(stringReading);

        if (udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
            UberdustNodeHandler.getInstance().observe(node, capability, this);
            response.setToken(udpRequest.getToken());
        }
        Random rand = new Random();

        response.setMID(rand.nextInt());

        byte[] buf = new byte[1024];
        final DatagramPacket replyPacket = new DatagramPacket(buf, 1024);
        replyPacket.setData(response.toByteArray());
        replyPacket.setSocketAddress(packet.getSocketAddress());
        LOGGER.info(packet.getSocketAddress());
        CoapServer.getInstance().sendReply(replyPacket);


    }
}
