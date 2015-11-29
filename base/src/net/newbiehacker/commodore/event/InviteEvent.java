package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.User;
import net.newbiehacker.commodore.Channel;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code InviteEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class InviteEvent extends IrcEvent {
    private MessageTransmitter sender;
    private User invitee;
    private Channel channel;

    public InviteEvent(IrcConnection source, MessageTransmitter sender, User invitedUser, Channel channel) {
        super(source);
        this.sender = sender;
        this.invitee = invitedUser;
        this.channel = channel;
    }

    public MessageTransmitter getSender() {
        return sender;
    }

    public User getTarget() {
         return invitee;
    }

    public Channel getChannel() {
        return channel;
    }

    public String toString() {
        return "InviteEvent[sender=" + sender + ",invitee=" + invitee + ",channel=" + channel + "]";
    }
}