package eu.uberdust.coap.udp;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.coap.CoapServer;
import eu.uberdust.coap.ConnectionManager;
import eu.uberdust.coap.Converter;
import eu.uberdust.coap.NodeHandler;
import eu.uberdust.coap.PropertyReader;
import eu.uberdust.coap.UberdustNodeHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.MalformedURLException;
import java.net.SocketAddress;
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
        LOGGER.info(udpOptions.size());
        for (Option udpOption : udpOptions) {
            LOGGER.info("udp-option: " + udpOption.getName());
        }


        String requestURI = udpRequest.getUriPath();
        if (udpRequest.getType() == Message.messageType.CON) {
//            sendAck(udpRequest.getMID(), packet.getSocketAddress());
        }

        System.out.println(requestURI);
        if (requestURI.equals("/.well-known/core")) {
            LOGGER.info("sending response");


            if (udpRequest.getCode() != CodeRegistry.METHOD_GET) return;

            Response response = createResponse(udpRequest);
            response.setContentType(MediaTypeRegistry.APPLICATION_LINK_FORMAT);

            LOGGER.info(response.getUriPath());
            LOGGER.info(response.getContentType());
            LOGGER.info(response.getMID());


            StringBuilder responsePayload = new StringBuilder("<.well-known/core>");
            Map<String, List<String>> nodes = NodeHandler.getInstance().getNodes();
            for (String s : nodes.keySet()) {
                for (String s1 : nodes.get(s)) {
                    if (s1.startsWith("urn")) {
                        responsePayload.append(",<").append(s).append("/").append(s1).append(">");
                    }
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


                    if (udpRequest.getCode() == CodeRegistry.METHOD_POST) {

                        String newuri = requestURI.substring(requestURI.indexOf("/", 1));
                        Request req = new Request(CodeRegistry.METHOD_POST, false);
                        req.setURI(newuri);
                        req.setPayload(udpRequest.getPayload());
                        LOGGER.info(Converter.getInstance().payloadToString(req.toByteArray()));
                        URL uberdustCall = null;
                        try {
                            uberdustCall = new URL("http://uberdust.cti.gr/rest/sendCommand/destination/" + node + "/payload/7f,69,70,33," + Converter.getInstance().payload(req.toByteArray()));
                            LOGGER.info("Calling " + uberdustCall);
                            uberdustCall.openConnection();
                            final InputStream input = uberdustCall.openStream();
                            int data = input.read();
                            StringBuilder sb = new StringBuilder();
                            while (data != -1) {
                                sb.append((char) data);
                                data = input.read();
                            }
                            input.close();
                        } catch (MalformedURLException e) {

                        } catch (IOException e) {

                        }

                        return;
                    }

                    URL uberdustReading;
                    LOGGER.info("Content type" + udpRequest.getContentType());

                    int ctype = -1;
                    if (udpRequest.hasOption(OptionNumberRegistry.ACCEPT)) {
                        List<Option> optionACC = udpRequest.getOptions(OptionNumberRegistry.ACCEPT);
                        for (Option option : optionACC) {
                            ctype = option.getIntValue();
                        }
                    }
                    if (ctype == 201) {
                        uberdustReading = new URL(PropertyReader.getInstance().getProperties().get("uberdustURL") + "/node/" + node + "/capability/" + capability + "/rdf/rdf-xml/limit/1");
                    } else {
                        uberdustReading = new URL(PropertyReader.getInstance().getProperties().get("uberdustURL") + "/node/" + node + "/capability/" + capability + "/latestreading");
                    }
                    uberdustReading.openConnection();
                    final InputStream input = uberdustReading.openStream();
                    int data = input.read();
                    StringBuilder sb = new StringBuilder();
                    while (data != -1) {
                        sb.append((char) data);
                        data = input.read();
                    }
                    input.close();
                    LOGGER.info("Should respond : ");


                    Response response = createResponse(udpRequest);
                    response.setContentType(udpRequest.getContentType());

                    response.setPayload(sb.toString());


                    LOGGER.info("reached here");

                    if (udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
                        UberdustNodeHandler.getInstance().observe(node, capability);
                        LOGGER.info("reached there");

                        ConnectionManager.getInstance().informFor(node, capability, packet, udpRequest);
                    } else {
                        ConnectionManager.getInstance().deregister(node, capability, packet);
                    }

                    LOGGER.info("reached over here");


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
        if (udpRequest.hasOption(OptionNumberRegistry.TOKEN)) {
            response.setToken(udpRequest.getToken());
        }


        response.setCode(CodeRegistry.RESP_CONTENT);
        response.setURI(udpRequest.getUriPath());
        response.setMID(udpRequest.getMID());
//        List<Option> options = udpRequest.getOptions();
//        for (Option option : options) {
//            LOGGER.info(option.getName());
//            response.addOption(option);
//        }
        response.setRequest(udpRequest);
        response.setType(Message.messageType.ACK);

        response.setContentType(udpRequest.getContentType());
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
            if (data[i] != 0) {
                myLen = i + 1;
                break;
            }
        }
        final byte[] adata = new byte[myLen];
        System.arraycopy(data, 0, adata, 0, myLen);
        LOGGER.info(adata.length);
        return adata;
    }


}
