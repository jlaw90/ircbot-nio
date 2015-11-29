package net.newbiehacker.bot;

import net.newbiehacker.commodore.User;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code UserManager}
 *
 * @author James Lawrence
 * @version 1
 */
public final class UserManager {
    private static List<BotUser> users = new ArrayList<BotUser>();

    public static synchronized BotUser getUser(User u, boolean acceptignored) {
        if(!u.isIdentified())
            return null;
        String serv = u.getConnection().getHost().getHostName().toLowerCase();
        for(BotUser bu: users)
            if(bu.nick.equalsIgnoreCase(u.getNick()) && bu.serv.equals(serv)) {
                bu.load();
                if(!acceptignored && bu.ignored)
                    return null;
                return bu;
            }
        BotUser bu = new BotUser(u.getNick(), serv);
        users.add(bu);
        if(!acceptignored && bu.ignored)
            return null;
        return bu;
    }

    public static synchronized BotUser getUser(User u) {
        return getUser(u, false);
    }
}