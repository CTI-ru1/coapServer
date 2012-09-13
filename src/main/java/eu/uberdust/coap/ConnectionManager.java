package eu.uberdust.coap;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.communication.websocket.readings.WSReadingsClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 *
 */
public class ConnectionManager implements Observer {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class);
    private static ConnectionManager ourInstance = new ConnectionManager();

    private transient final Map<String, List<Connection>> connections;

    private ConnectionManager() {
        connections = new HashMap<String, List<Connection>>();
    }

    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    public void informFor(String node, String capability, DatagramPacket packet, Request request) {
        String key = node + capability;
        if (connections.get(key) == null) {
            List<Connection> connlist = new ArrayList<Connection>();
            connections.put(key, connlist);
        }
        Connection connection = new Connection();
        connection.setNode(node);
        connection.setCapability(capability);
        connection.setPacket(packet);
        connection.setRequest(request);

        connections.get(key).add(connection);


    }


    @Override
    public void update(Observable o, Object arg) {
        if (!(o instanceof WSReadingsClient)) {
            return;
        }
        if (!(arg instanceof eu.uberdust.communication.protobuf.Message.NodeReadings)) {
            return;
        }
        eu.uberdust.communication.protobuf.Message.NodeReadings readings = (eu.uberdust.communication.protobuf.Message.NodeReadings) arg;
        for (eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading : readings.getReadingList()) {

            String key = reading.getNode() + reading.getCapability();
            List<Connection> cons = connections.get(key);
            LOGGER.info(key + "@" + cons.size());
            if (reading.hasDoubleReading()) {
                for (Connection con : cons) {
                    notifyFor(con, String.valueOf(reading.getDoubleReading()));
                }
            } else if (reading.hasStringReading()) {
                for (Connection con : cons) {
                    notifyFor(con, reading.getStringReading());
                }
            }
        }
    }

    private void notifyFor(Connection con, String stringReading) {

        LOGGER.info("Will notify:" + con);
        Response response = createResponse(con.getRequest());
        response.setContentType(MediaTypeRegistry.TEXT_PLAIN);

        if (con.getRequest().getContentType() == MediaTypeRegistry.APPLICATION_RDF_XML) {
            try {
                URL uberdustReading = new URL(PropertyReader.getInstance().getProperties().get("uberdustURL") + "/node/" + con.getNode() + "/rdf/rdf/");
                uberdustReading.openConnection();
                final InputStream input = uberdustReading.openStream();
                int data = input.read();
                StringBuilder sb = new StringBuilder();
                while (data != -1) {
                    sb.append((char) data);
                    data = input.read();
                }
                input.close();
                response.setPayload(sb.toString());
                response.setContentType(MediaTypeRegistry.APPLICATION_RDF_XML);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return;
            }

        } else {
            response.setPayload(stringReading);
        }

        Random rand = new Random();

        response.setMID(rand.nextInt());

        byte[] buf = new byte[1024];
        final DatagramPacket replyPacket = new DatagramPacket(buf, 1024);
        replyPacket.setData(response.toByteArray());
        replyPacket.setSocketAddress(con.getPacket().getSocketAddress());
        LOGGER.info(con.getPacket().getSocketAddress());
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

    public void deregister(String node, String capability, DatagramPacket packet) {
//return;
        String key = node + capability;
        List<Connection> cons = connections.get(key);
        if (cons != null) {
            LOGGER.info("CONS BEFORE=" + cons.size());
            for (Connection con : cons) {
                if (con.getPacket().getAddress().getHostAddress().equals(packet.getAddress().getHostAddress())) {
                    LOGGER.info("Removing information for " + key);
                    cons.remove(con);
                    break;
                }
            }
            cons = connections.get(key);
            LOGGER.info("CONS AFTER=" + cons.size());
        }

    }
}
