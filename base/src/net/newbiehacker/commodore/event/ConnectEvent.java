package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.IrcConnection;

/**
 * The {@code ConnectEvent} is fired when the library receives its first numeric reply from the IRC server</br>
 * If a listener receives this event, then it should safely be able to join channels or do whatever else it needs to on startup
 *
 * @author James Lawrence
 * @version 1
 */
public final class ConnectEvent extends IrcEvent {
    /**
     * Constructs a new {@code ConnectEvent}
     * @param source the connection that we are now connected to
     */
    public ConnectEvent(IrcConnection source) {
        super(source);
    }

    public String toString() {
        return "ConnectEvent[source=" + getSource().getHost().getHostName() + ",time=" + getTime() + "]";
    }
}