package net.newbiehacker.commodore;

import java.util.*;

/**
 * {@code User}
 *
 * @author James Lawrence
 * @version 1
 */
public final class User implements MessageTransmitter, MessageReceiver {
    String nick, user, host, real;
    List<Character> modes;
    Map<String, Channel> channels;
    IrcConnection connection;
    Date idleSince, signonTime;
    boolean identified;

    User(String nick, IrcConnection conn) {
        this.nick = nick;
        this.modes = new ArrayList<Character>();
        this.channels = new HashMap<String, Channel>();
        this.connection = conn;
    }

    public MessageTransceiverType getType() {
        return MessageTransceiverType.USER;
    }

    public IrcConnection getConnection() {
        return connection;
    }

    public String getNick() {
        return nick;
    }

    public String getUser() {
        return user;
    }

    public String getHost() {
        return host;
    }

    public String getRealName() {
        return real;
    }

    public boolean isIdentified() {
        return identified;
    }

    public Date getSignonTime() {
        return signonTime;
    }

    public Date getIdleSince() {
        return idleSince;
    }

    public List<Character> getModes() {
        return Collections.unmodifiableList(modes);
    }

    public boolean hasMode(char mode) {
        return modes.contains(mode);
    }

    public boolean onChannel(String channel) {
        return channels.containsKey(channel);
    }

    public boolean equals(Object o) {
        return o instanceof User && ((User) o).nick.equalsIgnoreCase(nick);
    }

    public String toString() {
        return nick;
    }

    public void dispose() {
        nick = null;
        user = null;
        host = null;
        modes.clear();
        modes = null;
        channels.clear();
        channels = null;
    }
}