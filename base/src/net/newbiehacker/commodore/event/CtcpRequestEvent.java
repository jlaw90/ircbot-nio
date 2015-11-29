package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.MessageReceiver;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code CtcpRequestEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class CtcpRequestEvent extends IrcEvent {
    private MessageTransmitter sender;
    private MessageReceiver target;
    private String request;

    public CtcpRequestEvent(IrcConnection source, MessageTransmitter sender, MessageReceiver target, String request) {
        super(source);
        this.sender = sender;
        this.target = target;
        this.request = request;
    }

    public MessageTransmitter getSender() {
        return sender;
    }

    public MessageReceiver getTarget() {
        return target;
    }

    public String getRequest() {
        return request;
    }

    public String toString() {
        return "CtcpRequestEvent[from=" + sender + ",to=" + target + ",request='" + request + "']";
    }
}