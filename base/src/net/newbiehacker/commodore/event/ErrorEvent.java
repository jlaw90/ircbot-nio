package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.IrcConnection;

/**
 * The {@code ErrorEvent} is fired when we receive an "ERROR" message from the server<br/>
 *
 * @author James Lawrence
 * @version 1
 */
public final class ErrorEvent extends IrcEvent {
    private String message;

    /**
     * Constructs a new {@code ErrorEvent}
     * @param source the connection from which we received this error
     * @param message the message body of the error
     */
    public ErrorEvent(IrcConnection source, String message) {
        super(source);
        this.message = message;
    }

    /**
     * Returns the message body of the error
     * @return the message body of the error
     */
    public String getMessage() {
        return message;
    }

    public String toString() {
        return "ErrorEvent[source=" + getSource().getHost().getHostName() + ",time=" + getTime() + ",message='" +
                message + "']";
    }
}