package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.MessageTransmitter;
import net.newbiehacker.commodore.MessageReceiver;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code NoticeEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class NoticeEvent extends IrcEvent {
    private MessageTransmitter sender;
    private MessageReceiver target;
    private String content;

    public NoticeEvent(IrcConnection source, MessageTransmitter sender, MessageReceiver target, String content) {
        super(source);
        this.sender = sender;
        this.target = target;
        this.content = content;
    }

    public MessageTransmitter getSender() {
        return sender;
    }

    public MessageReceiver getTarget() {
        return target;
    }

    public String getNotice() {
        return content;
    }

    public String toString() {
        return "NoticeEvent[from=" + sender + ",to=" + target + ",content='" + content + "']";
    }
}