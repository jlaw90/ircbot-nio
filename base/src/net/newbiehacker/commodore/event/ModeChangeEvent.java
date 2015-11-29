package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.MessageReceiver;
import net.newbiehacker.commodore.IrcConnection;

/**
 * The {@code ModeChangeEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class ModeChangeEvent extends IrcEvent {
    private MessageTransmitter sender;
    private MessageReceiver target;
    private String mode, param;

    public ModeChangeEvent(IrcConnection source, MessageTransmitter sender, MessageReceiver target, String mode, String param) {
        super(source);
        this.sender = sender;
        this.target = target;
        this.mode = mode;
        this.param = param;
    }

    public MessageTransmitter getSender() {
        return sender;
    }

    public MessageReceiver getTarget() {
        return target;
    }

    public String getModeString() {
        return mode;
    }

    public String getParam() {
        return param;
    }

    public String toString() {
        return "ModeChangeEvent[from=" + sender + ",to=" + target + ",mode=" + mode + ",param='" + param + "']";
    }
}