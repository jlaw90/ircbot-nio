package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.User;
import net.newbiehacker.commodore.IrcConnection;

/**
 * {@code NickChangeEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class NickChangeEvent extends IrcEvent {
    private String oldNick;
    private User user;

    public NickChangeEvent(IrcConnection source, String oldNick, User user) {
        super(source);
        this.oldNick = oldNick;
        this.user = user;
    }

    public String getOldNick() {
        return oldNick;
    }

    public User getUser() {
        return user;
    }

    public String toString() {
        return "NickChangeEvent[oldNick=" + oldNick + ",user=" + user + "]";
    }
}