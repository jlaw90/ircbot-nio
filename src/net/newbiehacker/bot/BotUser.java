package net.newbiehacker.bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * {@code BotUser}
 *
 * @author James Lawrence
 * @version 1
 */
public final class BotUser {
    String nick, serv;
    int groupFlags;
    boolean ignored;
    Properties properties;

    BotUser(String nick, String serv) {
        this.nick = nick;
        this.serv = serv;


        properties = new Properties();

        // Set default properties
        if(!load()) {
            properties.setProperty("group_flags", "0");
            properties.setProperty("ignored", "false");
            save();
        }

        load();
    }

    public String getNick() {
        return nick;
    }

    public String getConnectionHost() {
        return serv;
    }

    public void set(String key, String value) {
        key = key.toLowerCase();
        properties.put(key, value);
        save();
    }

    public String get(String key, String def) {
        key = key.toLowerCase();
        if(!properties.containsKey(key)) {
            set(key, def);
        }
        return properties.getProperty(key);
    }

    public int getInt(String key, int def) {
        return Integer.parseInt(get(key, String.valueOf(def)));
    }

    public boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(get(key, String.valueOf(def)));
    }

    public boolean load() {
        File f = new File("data/users/" + serv + "/" + nick + ".xml");
        if(!f.exists() || !f.canRead())
            return false;
        try {
            FileInputStream fis = new FileInputStream(f);
            properties.loadFromXML(fis);
            fis.close();


            // Parse constantly used properties for higher efficiency...
            ignored = Boolean.parseBoolean(properties.getProperty("ignored"));
            groupFlags = Integer.parseInt(properties.getProperty("group_flags", "0"));
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean save() {
        File dir = new File("data/users/" + serv + "/");
        if((!dir.exists() || !dir.isDirectory()) && !dir.mkdirs())
            return false;
        File f = new File(dir, nick + ".xml");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            properties.storeToXML(fos, "Bot user file");
            fos.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String toString() {
        return "BU:" + nick;
    }
}