package net.newbiehacker.bot;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code CommandManager}
 *
 * @author James Lawrence
 * @version 1
 */
public class CommandManager {
    private static List<Command> commands;

    static {
        commands = new ArrayList<Command>();
        load();
    }

    public static synchronized void load() {
        SAXBuilder sb = new SAXBuilder(true);
        commands.clear();
        try {
            InputStream is = new FileInputStream("data/commands.xml");
            Document d = sb.build(is);
            is.close();
            Element root = d.getRootElement();
            if (!root.getName().equalsIgnoreCase("commands"))
                throw new RuntimeException("Invalid commands.xml!");

            List<Element> children = root.getChildren("command");
            Element e1;
            main:
            for (Element e : children) {
                String name = e.getAttributeValue("name");
                String[] groups = e.getAttributeValue("groups").split(",\\s?");
                String[] params = new String[0];
                if (e.getAttribute("params") != null) {
                    params = e.getAttributeValue("params").split(",\\s?");
                    // Validate parameters...
                    boolean hadNonMandatory = false;
                    for (int i = 0; i < params.length; i++) {
                        if(params[i].endsWith("?")) {
                            hadNonMandatory = true;
                        } else if(hadNonMandatory) {
                            System.err.println("Invalid command parameter format for " + name + ", ignoring...");
                            continue main;
                        }
                        if ((params[i].endsWith("...") || params[i].endsWith("...?")) && i != params.length - 1) {
                            System.err.println("Invalid command parameter format for " + name + ", ignoring...");
                            continue main;
                        }
                    }
                }

                String help = null;
                if ((e1 = e.getChild("help")) != null)
                    help = e1.getTextTrim();
                if ((e1 = e.getChild("script")) != null) {
                    String lang = e1.getAttributeValue("lang");
                    String script;
                    if (e1.getAttribute("src") != null) {
                        // Load script from file...
                        try {
                            File f = new File("data/scripts/" + e1.getAttributeValue("src"));
                            FileInputStream fis = new FileInputStream(f);
                            byte[] data = new byte[(int) f.length()];
                            int off = 0;
                            while (off != data.length)
                                off += fis.read(data, off, data.length - off);
                            script = new String(data);
                            data = null;
                            fis.close();
                        } catch (Throwable t) {
                            t.printStackTrace();
                            continue;
                        }
                    } else
                        script = e1.getText();

                    commands.add(new Command(name, groups, params, help, lang, script));
                }
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Command findCommand(String name) {
        for (Command c : commands)
            if (c.getName().equalsIgnoreCase(name))
                return c;
        return null;
    }

    public static synchronized List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}