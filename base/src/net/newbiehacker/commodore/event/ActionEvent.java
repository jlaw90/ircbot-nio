package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.MessageReceiver;
import net.newbiehacker.commodore.IrcConnection;

/**
 * An {@code ActionEvent} is an event that is fired after a {@code MessageTransmitter} sends an action to a {@code MessageReceiver}
 *
 * @author James Lawrence
 * @version 1
 */
public final class ActionEvent extends IrcEvent {
    private MessageTransmitter sender;
    private MessageReceiver target;
    private String action;

    /**
     * Constructs a new {@code ActionEvent}
     * @param source the connection from which we received an action
     * @param sender the sender of the action
     * @param target the receiver of the action
     * @param action the action
     */
    public ActionEvent(IrcConnection source, MessageTransmitter sender, MessageReceiver target, String action) {
        super(source);
        this.sender = sender;
        this.target = target;
        this.action = action;
    }

    /**
     * Returns the {@code MessageTransmitter} that sent the action
     * @return what sent the action
     */
    public MessageTransmitter getSender() {
        return sender;
    }

    /**
     * Returns the {@code MessageReceiver} that received the action
     * @return what received the action
     */
    public MessageReceiver getTarget() {
        return target;
    }

    /**
     * Returns the action
     * @return the action
     */
    public String getAction() {
        return action;
    }

    public String toString() {
        return "ActionEvent[from=" + sender + ",to=" + target + ",content='" + action + "']";
    }
}