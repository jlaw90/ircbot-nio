package net.newbiehacker.commodore;

import java.util.*;

/**
 * {@code Channel}
 *
 * @author James Lawrence
 * @version 1
 */
public final class Channel implements MessageReceiver {
    Date topicSetTime, creationTime;
    List<User> users;
    Map<Character, List<ListEntry>> lists;
    Map<Character, String> modes;
    Map<User, List<Character>> rights;
    String name, topic, topicSetter;

    Channel(String name) {
        this.name = name;
        this.topic = "";
        this.users = new ArrayList<User>();
        this.rights = new HashMap<User, List<Character>>();
        this.modes = new HashMap<Character, String>();
        this.lists = new HashMap<Character, List<ListEntry>>();
    }

    public void dispose() {
        name = topic = null;
        users.clear();
        users = null;
        modes.clear();
        modes = null;
        for(List<ListEntry> l: lists.values())
            l.clear();
        lists.clear();
        lists = null;
    }

    public boolean equals(Object o) {
        return o instanceof Channel && ((Channel) o).name.equalsIgnoreCase(name);
    }

    public List<ListEntry> getList(char mode) {
        return Collections.unmodifiableList(lists.get(mode));
    }

    public String getModeParameter(char mode) {
        return modes.get(mode);
    }

    public String getName() {
        return name;
    }

    public List<Character> getRights(User u) {
        if(rights.get(u) == null)
            rights.put(u, new ArrayList<Character>());
        return Collections.unmodifiableList(rights.get(u));
    }

    public String getTopic() {
        return topic;
    }

    public String getTopicSetter() {
        return topicSetter;
    }

    public Date getTopicSetTime() {
        return topicSetTime;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public MessageTransceiverType getType() {
        return MessageTransceiverType.CHANNEL;
    }

    public User getUser(String nick) {
        for(User u: users)
            if(u.getNick().equalsIgnoreCase(nick))
                return u;
        return null;
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public boolean hasMode(char mode) {
        return modes.containsKey(mode);
    }

    public List<Character> getModes() {
        return Collections.unmodifiableList(new ArrayList<Character>(modes.keySet()));
    }

    public boolean hasRight(User u, char right) {
        return rights.get(u).contains(right);
    }

    public boolean hasUser(User u) {
        return users.contains(u);
    }

    public boolean hasUser(String nick) {
        for(User u: users)
            if(u.getNick().equalsIgnoreCase(nick))
                return true;
        return false;
    }

    public String toString() {
        return name;
    }
}