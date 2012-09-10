package eu.uberdust.coap.udp;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.coap.CoapServer;
import eu.uberdust.coap.NodeHandler;
import eu.uberdust.coap.PropertyReader;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class CoapUdpRequestHandler implements Runnable {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpRequestHandler.class);

    private transient final DatagramPacket packet;

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
        final Request udpRequest = (Request) Request.fromByteArray(inData);

        List<Option> udpOptions = udpRequest.getOptions();
        for (Option udpOption : udpOptions) {
            LOGGER.info("udp-option: " + udpOption.getName());
        }

        String requestURI = udpRequest.getUriPath();

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

        } else {

            LOGGER.info("should go here");
            try {
                String[] uriparts = requestURI.split("/");
                if (uriparts.length == 3) {
                    String node = uriparts[1];
                    String capability = uriparts[2];
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
}
