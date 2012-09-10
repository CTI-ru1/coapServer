package eu.uberdust.coap;

import org.apache.log4j.Logger;

import java.net.SocketAddress;

/**
 * Contains all information about an active request from a source.
 */
public class ActiveRequest {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ActiveRequest.class);

    private transient final String uriPath;
    private transient final int mid;
    private transient final String token;
    private transient final String host;
    private transient final SocketAddress socketAddress;
    private transient final boolean query;

    /**
     * @return the uri path of the request
     */
    public String getUriPath() {
        return uriPath;
    }

    /**
     * @return the id of the message
     */
    public int getMid() {
        return mid;
    }

    /**
     * @return the token of the query if it exists
     */
    public String getToken() {
        return token;
    }

    /**
     * @return the host concerning the query
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the socket address fo the sender
     */
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * @return true if the request was a query
     */
    public boolean hasQuery() {
        return query;
    }

    /**
     * Constructor.
     *
     * @param uriPath       the uri path
     * @param mid           the message id
     * @param token         the message token
     * @param host          the host of the message
     * @param socketAddress the socket address of the sender
     * @param query         true if it is a query
     */
    public ActiveRequest(final String uriPath, final int mid, final String token, final String host,
                         final SocketAddress socketAddress, final boolean query) {
        LOGGER.debug("new ActiveRequest");
        this.uriPath = uriPath;
        this.mid = mid;
        this.token = token;
        this.host = host;
        this.socketAddress = socketAddress;
        this.query = query;
    }
}
