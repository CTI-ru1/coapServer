package eu.uberdust.coap;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class NodeHandler extends TimerTask {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(NodeHandler.class);
    private static NodeHandler ourInstance = new NodeHandler();

    private final transient Map<String, List<String>> nodes;
    private static final long INTERVAL = 10 * 1 * 1000;

    public Map<String, List<String>> getNodes() {
        return nodes;
    }

    public static NodeHandler getInstance() {
        return ourInstance;
    }

    private NodeHandler() {
        nodes = new HashMap<String, List<String>>();
        checkNodes();

        Timer t = new Timer();
        t.scheduleAtFixedRate(this, 1000, INTERVAL);

        Timer t1 = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                Set<String> mnodes = nodes.keySet();
                for (String mnode : mnodes) {
                    List<String> properties = nodes.get(mnode);

                    for (String property : properties) {
                        if (!property.startsWith("urn")) continue;
                        try {
                            URL uberdustReading = new URL(PropertyReader.getInstance().getProperties().get("uberdustURL") + "/node/" + mnode + "/capability/" + property + "/rdf/rdf-xml/limit/1");
                            uberdustReading.openConnection();
                            final InputStream input = uberdustReading.openStream();
                            int data = input.read();
                            StringBuilder sb = new StringBuilder();
                            while (data != -1) {
                                sb.append((char) data);
                                data = input.read();
                            }
                            LOGGER.info("ping " + mnode + "-" + property);
                            input.close();
                        } catch (IOException e) {
                            LOGGER.trace(e.getMessage());
                        }


                    }
                }

            }
        }, 10000, 30 * 60 * 1000);
    }

    @Override
    public void run() {
        checkNodes();
    }

    private final void checkNodes() {
        LOGGER.info("UPDATE NODES");

        Map<String, List<String>> nodes = new HashMap<String, List<String>>();


        LOGGER.info("Updating nodes information");

        try {
            URL uberudstNodes = new URL(PropertyReader.getInstance().getProperties().get("uberdustURL") + "/status/raw");
            uberudstNodes.openConnection();
            final InputStream input = uberudstNodes.openStream();
            int data = input.read();
            StringBuilder sb = new StringBuilder();
            while (data != -1) {
                sb.append((char) data);
                data = input.read();
            }
            input.close();
            String[] response = sb.toString().split("\n");

            for (String s : response) {
                List<String> capabilities = new ArrayList<String>();
                nodes.put(s.split("\t")[0], capabilities);
            }
            for (String s : nodes.keySet()) {

                for (String s1 : response) {
                    String[] parts = s1.split("\t");
                    if (parts[0].equals(s)) {
                        nodes.get(s).add(parts[1]);
                    }
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        LOGGER.info("Found " + nodes.keySet().size() + " nodes");
        //update overal nodes
        this.nodes.clear();
        this.nodes.putAll(nodes);
    }
}
