package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.IrcConnection;

/**
 * The {@code DisconnectEvent} is fired when we are disconnected from the server<br/>
 * This allows cleanup or reconnection to be executed by a listener
 *
 * @author James Lawrence
 * @version 1
 */
public final class DisconnectEvent extends IrcEvent {
    /**
     * Constructs a new {@code DisconnectEvent}
     * @param source the connection that we disconnected from
     */
    public DisconnectEvent(IrcConnection source) {
        super(source);
    }

    public String toString() {
        return "DisconnectEvent[source=" + getSource().getHost().getHostName() + ",time=" + getTime() + "]";
    }
}