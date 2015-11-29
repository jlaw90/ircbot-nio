package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.Channel;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code TopicChangeEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class TopicChangeEvent extends IrcEvent {
    private Channel channel;

    public TopicChangeEvent(IrcConnection source, Channel channel) {
        super(source);
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public String toString() {
        return "TopicChangeEvent[source=" + getSource().getHost().getHostName() + ",time=" + getTime() + ",channel=" +
                channel + "]";
    }
}