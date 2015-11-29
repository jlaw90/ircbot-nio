package net.newbiehacker.commodore;

/**
 * {@code Server}
 *
 * @author James Lawrence
 * @version 1
 */
public final class Server implements MessageTransmitter {
    private String host;

    public Server(String host) {
        this.host = host;
    }

    public MessageTransceiverType getType() {
        return MessageTransceiverType.SERVER;
    }

    public String getHost() {
        return host;
    }

    public void dispose() {
        this.host = null;
    }

    public boolean equals(Object o) {
        return o instanceof Server && ((Server) o).host.equalsIgnoreCase(host);
    }

    public String toString() {
        return host;
    }
}