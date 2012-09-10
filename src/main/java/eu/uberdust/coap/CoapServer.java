package eu.uberdust.coap;

import eu.uberdust.coap.udp.UDPhandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Random;

/**
 *
 */
public class CoapServer {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapServer.class);
    /**
     * Singleton instance.
     */
    private static CoapServer instance = null;

    /**
     * COAP Server socket.
     */
    private transient DatagramSocket socket;

    /**
     * Random number generator.
     */
    private transient final Random mid;


    private static final long MILLIS_TO_STALE = 3 * 60 * 1000;

//    private static final String URL="http://uberdust.cti.gr/rest/testbed/";
    /**
     * Constructor.
     */
    public CoapServer() {

        PropertyReader.getInstance().setFile("coap.properties");

        NodeHandler.getInstance();

        mid = new Random();

        //Start the udp socket
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            LOGGER.error(e.getMessage(), e);
        }

        //Start the handler
        final UDPhandler thread = new UDPhandler(socket);
        thread.start();

        LOGGER.info("started CoapServer");
    }

    /**
     * Singleton Class.
     *
     * @return The unique instance of CoapServer.
     */
    public static CoapServer getInstance() {
        synchronized (CoapServer.class) {
        LOGGER.info("get instance");
        if (instance == null) {
            instance = new CoapServer();
        }
        }
        return instance;
    }


    /**
     * Sends a reply to a packet using the socket.
     *
     * @param replyPacket the packet to use to reply.
     */

    public void sendReply(final DatagramPacket replyPacket) {
        LOGGER.info("sent response");
        synchronized (CoapServer.class) {
        try {

            LOGGER.info("sent response");
            socket.send(replyPacket);
            LOGGER.info("sent response");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
        }
        }
    }

    /**
     * Sends a reply to a packet using the socket.
     *
     * @param buf           the bytes to send as a reply
     * @param socketAddress the address of the udp client
     */
    public void sendReply(final byte[] buf, final SocketAddress socketAddress) {
        final DatagramPacket replyPacket = new DatagramPacket(buf, buf.length);
        replyPacket.setData(buf);
        replyPacket.setSocketAddress(socketAddress);

        synchronized (CoapServer.class) {
            try {
                socket.send(replyPacket);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Testing Main function.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        CoapServer.getInstance();
    }

}
