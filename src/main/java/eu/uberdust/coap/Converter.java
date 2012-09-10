package eu.uberdust.coap;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/22/12
 * Time: 12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class Converter {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(
            Converter.class);

    private static Converter instance = null;

    public static Converter getInstance() {
        synchronized (Converter.class) {
            if (instance == null) {
                instance = new Converter();
            }
        }
        return instance;
    }

    public int[] addressToInteger(final String address) {
        final int[] macAddress = new int[2];
        if (address.length() == 4) {
            macAddress[0] = Integer.valueOf(address.substring(0, 2), 16);
            macAddress[1] = Integer.valueOf(address.substring(2, 4), 16);
        } else if (address.length() == 3) {
            macAddress[0] = Integer.valueOf(address.substring(0, 1), 16);
            macAddress[1] = Integer.valueOf(address.substring(1, 3), 16);
        }
        return macAddress;
    }

    public byte[] addressToByte(final String address) {
        final byte[] macAddress = new byte[2];
        if (address.length() == 4) {
            macAddress[0] = Integer.valueOf(address.substring(0, 2), 16).byteValue();
            macAddress[1] = Integer.valueOf(address.substring(2, 4), 16).byteValue();
        } else if (address.length() == 3) {
            macAddress[0] = Integer.valueOf(address.substring(0, 1), 16).byteValue();
            macAddress[1] = Integer.valueOf(address.substring(1, 3), 16).byteValue();
        }
        return macAddress;
    }

    public String payloadToString(final int[] payload) {
        final StringBuilder stringBuilder = new StringBuilder("Contents:");
        for (int i : payload) {
            stringBuilder.append(Integer.toHexString(i)).append("|");
        }

        return stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
    }

    public int[] toIntArray(byte[] bytes) {
        return new int[0];  //To change body of created methods use File | Settings | File Templates.
    }

    public String payloadToString(final byte[] payload) {
        final StringBuilder stringBuilder = new StringBuilder("Contents:");
        for (int i : payload) {
            stringBuilder.append(Integer.toHexString(i)).append("|");
        }

        return stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
    }

    public int[] ByteToInt(byte[] data) {
        final int[] bytes = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            final short read = (short) ((short) data[i] & 0xff);
            bytes[i] = read;
            if (bytes[i] == 255) {
                bytes[i] = 0;
            }

        }
        return bytes;
    }

    public static String[] extractCapabilities(final String message) {
        LOGGER.info(message);
        String[] capabilities = message.substring(1).split("<");
        LOGGER.info("cap found " + capabilities.length);
        for (int i = 0; i < capabilities.length; i++) {
            capabilities[i] = capabilities[i].substring(0, capabilities[i].indexOf('>'));
        }
        return capabilities;
    }
}
