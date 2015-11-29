package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.User;
import net.newbiehacker.commodore.Channel;
import net.newbiehacker.commodore.IrcConnection;

/**
 * The {@code JoinEvent} is fired when a user joins a channel
 *
 * @author James Lawrence
 * @version 1
 */
public final class JoinEvent extends IrcEvent {
    private Channel channel;
    private User user;

    /**
     * Constructs a new {@code JoinEvent}
     * @param ic the connection that fired this event
     * @param channel the channel that was joined
     * @param u the user that joined
     */
    public JoinEvent(IrcConnection ic, Channel channel, User u) {
        super(ic);
        this.channel = channel;
        this.user = u;
    }

    /**
     * Returns the channel that was joined
     * @return the channel that was joined
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Returns the user that joined
     * @return the user that joined
     */
    public User getUser() {
        return user;
    }

    public String toString() {
        return "JoinEvent[channel=" + channel + ",user=" + user + "]";
    }
}