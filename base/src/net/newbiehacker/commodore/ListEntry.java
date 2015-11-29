package net.newbiehacker.commodore;

import java.util.Date;

/**
 * {@code ListEntry}
 *
 * @author James Lawrence
 * @version 1
 */
public final class ListEntry {
    private String mask, setter;
    private Date time;

    public ListEntry(String setter, String mask, Date time) {
        this.setter = setter;
        this.mask = mask;
        this.time = time;
    }

    public String getMask() {
        return mask;
    }

    public String getSetter() {
        return setter;
    }

    public Date getTime() {
        return time;
    }

    public boolean equals(Object o) {
        if(!(o instanceof ListEntry))
            return false;
        ListEntry le = (ListEntry) o;
        return mask.equals(le.mask) && setter.equals(le.setter) && time.equals(le.time);
    }

    public boolean matches(User u) {
        // Todo: look up mask format more closely
        String regString = mask.replace(".", "\\.").replace("[", "\\[").replace("]", "\\]").replace("^", "\\^").replace("*", ".*");
        return (u.getNick() + "!" + u.getUser() + "@" + u.getHost()).matches(regString);
    }


    public String toString() {
        return "'" + mask + "', set by " + setter + " on " + time;
    }
}