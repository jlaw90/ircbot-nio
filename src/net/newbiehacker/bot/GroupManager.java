package net.newbiehacker.bot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code GroupManager}
 *
 * @author James Lawrence
 * @version 1
 */
public final class GroupManager {
    private static final List<Group> groups;

    static {
        groups = new ArrayList<Group>();
        load();
    }

    public static synchronized void addGroup(String group, int flag) {
        groups.add(new Group(flag, group));
        save();
    }

    public static synchronized void removeGroup(String group) {
        for(Group g: groups) {
            if(g.name.equalsIgnoreCase(group)) {
                groups.remove(g);
                save();
                return;
            }
        }
    }

    public static synchronized void addGroup(BotUser b, String group) {
        for(Group g: groups) {
            if(g.name.equalsIgnoreCase(group)) {
                b.groupFlags |= g.flag;
                b.save();
                return;
            }
        }
    }

    public static synchronized void removeGroup(BotUser b, String group) {
        for(Group g: groups) {
            if(g.name.equalsIgnoreCase(group)) {
                b.groupFlags ^= g.flag;
                b.save();
                return;
            }
        }
    }

    public static synchronized boolean inGroup(BotUser b, String group) {
        for(Group g: groups)
            if(g.name.equalsIgnoreCase(group)) {
                return (b.groupFlags & g.flag) == g.flag;
            }
        return false;
    }

    public static synchronized List<String> getGroups(BotUser user) {
        int flags = user.groupFlags;

        List<String> groups = new ArrayList<String>();

        for(Group g: GroupManager.groups)
            if((flags & g.flag) == g.flag)
                groups.add(g.name);
        return groups;
    }

    public static List<Group> getGroups() {
        return groups;
    }

    public static synchronized void load() {
        groups.clear();
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader("data/groups.cfg"));
            while((line = br.readLine()) != null) {
                if(line.equals(""))
                    continue;
                int idx = line.indexOf('=');
                if(idx == -1)
                    continue;
                int bitflag = Integer.parseInt(line.substring(idx + 1));
                String name = line.substring(0, idx);
                groups.add(new Group(bitflag, name));
            }
            br.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void save() {
        try {
            PrintStream ps = new PrintStream("data/groups.cfg");
            for(Group g: groups) {
                ps.println(g.name + "=" + g.flag);
            }
            ps.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class Group {
        int flag;
        String name;

        public Group(int flag, String name) {
            this.flag = flag;
            this.name = name;
        }

        public int getFlag() {
            return flag;
        }

        public String getName() {
            return name;
        }
    }
}