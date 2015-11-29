package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.IrcConnection;
import net.newbiehacker.commodore.User;

/**
 * {@code QuitEvent}
 *
 * @author James Lawrence
 * @version 1
 */
public final class QuitEvent extends IrcEvent {
    private User user;
    private String message;

    public QuitEvent(IrcConnection source, User user, String message) {
        super(source);
        this.user = user;
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public boolean hasMessage() {
        return message != null;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "QuitEvent[user=" + user + ",message='" + message + "']";
    }
}