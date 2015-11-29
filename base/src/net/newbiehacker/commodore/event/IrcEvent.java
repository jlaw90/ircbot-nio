package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.IrcConnection;
import java.util.Date;

/**
 * The {@code IrcEvent} class is the superclass of all the events fired by this library and so holds arguments which all
 * events will need
 *
 * @author James Lawrence
 * @version 1
 */
public abstract class IrcEvent {
    private IrcConnection source;
    private Date time;

    /**
     * Constructs a new {@code IrcEvent}
     * @param source the connection that fired this event
     */
    public IrcEvent(IrcConnection source) {
        this.source = source;
        this.time = new Date();
    }

    /**
     * Returns the connection that fired this event
     * @return the connection that fired this event
     */
    public IrcConnection getSource() {
        return source;
    }

    /**
     * Returns a {@code Date} object with the time of when this event was constructed
     * @return the time when this event was constructed
     */
    public Date getTime() {
        return time;
    }
}