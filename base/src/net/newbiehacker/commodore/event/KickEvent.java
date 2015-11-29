package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.Channel;
import net.newbiehacker.commodore.User;
import net.newbiehacker.commodore.IrcConnection;
import net.newbiehacker.commodore.MessageTransmitter;

/**
 * {@code KickEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class KickEvent extends IrcEvent {
    private MessageTransmitter kicker;
    private Channel channel;
    private User kicked;
    private String message;

    public KickEvent(IrcConnection source, MessageTransmitter kicker, Channel channel, User kicked, String message) {
        super(source);
        this.kicker = kicker;
        this.channel = channel;
        this.kicked = kicked;
        this.message = message;
    }

    public MessageTransmitter getKicker() {
        return kicker;
    }

    public Channel getChannel() {
        return channel;
    }

    public User getKickedUser() {
        return kicked;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "KickEvent[channel=" + channel + ",kicker=" + kicker + ",kicked=" + kicked + ",message='" + message + "']";
    }
}