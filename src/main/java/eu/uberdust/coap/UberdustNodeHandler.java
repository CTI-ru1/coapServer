package eu.uberdust.coap;

import eu.uberdust.communication.websocket.readings.WSReadingsClient;
import org.apache.log4j.Logger;

import java.util.Observable;
import java.util.Observer;

/**
 *
 */
public class UberdustNodeHandler implements Observer {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(UberdustNodeHandler.class);
    private static UberdustNodeHandler ourInstance = new UberdustNodeHandler();


    private UberdustNodeHandler() {
        LOGGER.info((String) PropertyReader.getInstance().getProperties().get("uberdust.websocket"));
        WSReadingsClient.getInstance().setServerUrl((String) PropertyReader.getInstance().getProperties().get("uberdust.websocket"));
        WSReadingsClient.getInstance().addObserver(ConnectionManager.getInstance());

    }


    public static UberdustNodeHandler getInstance() {
        return ourInstance;
    }


    @Override
    public void update(Observable o, Object arg) {

    }

    public void observe(String node, String capability) {
        WSReadingsClient.getInstance().subscribe(node, capability);

    }
}
