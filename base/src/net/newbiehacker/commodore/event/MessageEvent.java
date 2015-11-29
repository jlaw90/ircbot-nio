package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.IrcConnection;
import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.MessageReceiver;

/**
 * The {@code MessageEvent} is fired when we receive a message from a user or a channel
 *
 * @author James Lawrence
 * @version 1
 */
public final class MessageEvent extends IrcEvent {
    private MessageTransmitter sender;
    private MessageReceiver target;
    private String content;

    /**
     * Constructs a new {@code MessageEvent}
     * @param source the connection that fired this event
     * @param sender what sent this message
     * @param target what received this message
     * @param content the message body
     */
    public MessageEvent(IrcConnection source, MessageTransmitter sender, MessageReceiver target, String content) {
        super(source);
        this.sender = sender;
        this.target = target;
        this.content = content;
    }

    /**
     * Returns the {@code MessageTransmitter} that sent this message
     * @return what sent this message
     */
    public MessageTransmitter getSender() {
        return sender;
    }

    /**
     * Returns the {@code MessageReceiver} that received this message
     * @return what received this message
     */
    public MessageReceiver getTarget() {
        return target;
    }

    /**
     * Returns the message body
     * @return the message body
     */
    public String getMessage() {
        return content;
    }

    public String toString() {
        return "MessageEvent[from=" + sender + ",to=" + target + ",content='" + content + "']";
    }
}