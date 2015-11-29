package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.MessageReceiver;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code NumericMessageEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class NumericMessageEvent extends IrcEvent{
    private MessageTransmitter sender;
    private MessageReceiver target;
    private int numeric;
    private String content;

    public NumericMessageEvent(IrcConnection source, MessageTransmitter sender, MessageReceiver target, int numeric, String content) {
        super(source);
        this.sender = sender;
        this.target = target;
        this.numeric = numeric;
        this.content = content;
    }

    public MessageTransmitter getSender() {
        return sender;
    }

    public MessageReceiver getTarget() {
        return target;
    }

    public int getNumeric() {
        return numeric;
    }

    public String getMessage() {
        return content;
    }

    public String toString() {
        return "NumericMessageEvent[from=" + sender + ",to=" + target + ",numeric=" + numeric + ",content='" + content + "']";
    }
}