package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.Channel;
import net.newbiehacker.commodore.User;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code PartEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class PartEvent extends IrcEvent {
    private Channel channel;
    private User user;
    private String message;

    public PartEvent(IrcConnection source, Channel channel, User user, String message) {
        super(source);
        this.channel = channel;
        this.user = user;
        this.message = message;
    }

    public Channel getChannel() {
        return channel;
    }

    public User getUser() {
        return user;
    }

    public boolean hasMessage() {
        return message != null;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "PartEvent[channel=" + channel + ",user=" + user + ",message='" + message + "']";
    }
}