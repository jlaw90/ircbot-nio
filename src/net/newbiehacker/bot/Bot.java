package net.newbiehacker.bot;

import net.newbiehacker.commodore.*;
import net.newbiehacker.commodore.event.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.script.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code Bot}
 *
 * @author James Lawrence
 * @version 1
 */
public final class Bot implements IrcEventListener {
    public static boolean autoRehash = true;
    public static String prefix = "`";

    private ScriptEngineManager engineManager;
    private long lastConnect;
    private long connectLength = 5000;
    private IrcConnection connection;
    private int port;
    private String host, defNick, defName, defRealName;

    public static void main(String[] args) throws IOException {
        SAXBuilder b = new SAXBuilder(true);
        try {
            Document d = b.build(new File("data/config.xml"));

            Element root = d.getRootElement();
            if(!root.getName().equals("config"))
                throw new RuntimeException("Bot configuration (config.xml) invalid (no root element)!");

            @SuppressWarnings("unchecked cast") List<Element> servers = root.getChildren("server");

            for(Element e : servers) {
                String host = e.getAttributeValue("host");
                int port = Integer.decode(e.getAttributeValue("port", "6667"));
                String nick = e.getAttributeValue("nick", "CommodoreJ");
                String name = e.getAttributeValue("name", "CommodoreJ");
                String realname = e.getAttributeValue("realname", "CommodoreJ IRC bot");
                Bot bo = new Bot(host, port, nick, name, realname);
                bo.connect();
            }
        } catch(JDOMException e) {
            throw new RuntimeException("Bot configuration (config.xml) invalid!: " + e.getMessage());
        }
    }

    static {
        GroupManager.load();
        CommandManager.load();
    }

    public  Bot(String host, int port, String nick, String name, String realname) throws IOException {
        this.port = port;
        this.host = host;
        this.defNick = nick;
        this.defName = name;
        this.defRealName = realname;
        engineManager = new ScriptEngineManager();
    }

    private void connect() throws IOException {
        System.out.println("Connection initialised...");
        long time = System.currentTimeMillis();
        if(time - lastConnect <= connectLength) {
            // We've been disconnected quickly, add 5000 to connectLength
            connectLength += 5000;
            try {
                Thread.sleep(connectLength - (time - lastConnect));
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // We've been connected a while, reset connectLength to 5000
            connectLength = 5000;
        }
        lastConnect = time;
        if(connection != null && connection.isConnected()) {
            System.out.println("Already connected, closing old connection");
            connection.close();
        }
        System.out.println("Creating new connection...");
        connection = new IrcConnection(InetAddress.getByName(host), port);
        System.out.println("New connection created");
        connection.addListener(this);
        connection.setRakingInfo(true);
        connection.connect(defNick, defName, defRealName);
    }

    private void doReply(IrcConnection con, int reply_type, String dest, String message) {
        switch(reply_type) {
            case 0:
                con.sendMessage(dest, message);
                break;
            case 1:
                con.sendNotice(dest, message);
        }
    }

    private String from(String[] data, int off) {
        StringBuilder sb = new StringBuilder();
        for(int i = off; i < data.length; i++) {
            sb.append(data[i]);
            if(i != data.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }

    private ScriptContext getContext(String rto, int rt, User source, BotUser bsource, Map<String, Object> extra) {
        ScriptContext sc = new SimpleScriptContext();
        Bindings sb = new SimpleBindings();
        if(extra != null)
            sb.putAll(extra);
        sb.put("dest", rto);
        sb.put("rt", rt);
        sb.put("user", source);
        sb.put("botuser", bsource);
        System.out.println("Adding " + connection + " to bindings");
        sb.put("irc", connection);
        System.out.println("Added " + connection + " to bindings...");
        sb.put("bot", this);
        sc.setBindings(sb, ScriptContext.ENGINE_SCOPE);
        return sc;
    }

    private void processCommand(IrcConnection con, String cmd, String[] params, User source, int rt, String rto) {
        BotUser b = UserManager.getUser(source);
        if(b == null)
            return;
        if(autoRehash) {
            CommandManager.load();
        }

        // Eval command
        if(cmd.equals("eval")) {
            if(!GroupManager.inGroup(b, "admin"))
                return;
            if(params.length < 2) {
                doReply(con, rt, rto, "Usage: `eval [engine] script...");
                return;
            }
            String lang = params[0].toLowerCase();
            ScriptEngine se = engineManager.getEngineByExtension(lang);
            if(se == null) {
                doReply(con, rt, rto, "Usage: `eval [engine] script...");
                return;
            }
            try {
                StringBuilder pre = new StringBuilder(CommandManager.findCommand("common." + lang).getScript());
                pre.append(from(params, 1));
                doReply(con, rt, rto, "Eval: " + String.valueOf(se.eval(pre.toString(), getContext(rto, rt, source, b, null))));
            } catch(ScriptException e) {
                doReply(con, rt, rto, "Exception caught!");
                e.printStackTrace();
            }
            return;
        }

        // Help command...
        if(cmd.equals("help")) {
            if(params.length < 1) {
                // Build a list of all commands
                List<Command> list = CommandManager.getCommands();
                StringBuilder sb = new StringBuilder("Commands (prefix is '" + prefix + "'): ");
                boolean started = false;
                for(Command c : list) {
                    if(c.getName().startsWith("common.") || !c.canRun(b))
                        continue;
                    if(started)
                        sb.append(", ");
                    started = true;
                    sb.append(c.getName());
                }
                sb.append(", For more help please type ").append(prefix).append("help [command]");
                doReply(con, rt, rto, sb.toString());
                return;
            }
            String check = params[0];
            Command c = CommandManager.findCommand(check);
            if(c == null) {
                doReply(con, rt, rto, "No such command found.");
                return;
            }
            String[] parameters = c.getParameters();
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < parameters.length; i++) {
                if(i != 0)
                    sb.append(", ");
                sb.append(parameters[i]);
            }
            doReply(con, rt, rto, (parameters.length == 0? "No parameters": ("Parameters: " + sb.toString())) + "; " + (c.getHelp() == null? "Help is not available for this command.": c.getHelp()));
            return;
        }

        // Any other command...

        Command c = CommandManager.findCommand(cmd);
        if(c == null || !c.canRun(b))
            return;

        // Build parameter data and ensure we have all required...
        String[] parameters = c.getParameters();
        int manlength = 0;
        for(String parameter : parameters)
            if(!parameter.endsWith("?"))
                manlength++;
        if(params.length < manlength) {
            StringBuilder sb = new StringBuilder("Usage: ").append(prefix).append(c.getName()).append(" ");
            for(String p : parameters)
                sb.append(p).append(" ");
            if(c.getHelp() != null)
                sb.append("For more help please type ").append(prefix).append("help ").append(c.getName());
            doReply(con, rt, rto, sb.toString());
            return;
        }

        try {

            // Build parameter map...
            int lpoff = -1;
            Map<String, Object> paramMap = new HashMap<String, Object>();
            int max = Math.max(parameters.length, params.length);
            for(int i = 0; i < max; i++) {
                // Append variable length parameter string
                if(lpoff != -1) {
                    if(i < params.length)
                        paramMap.put(parameters[lpoff], paramMap.get(parameters[lpoff]) + " " + params[i]);
                    continue;
                }
                // If we haven't defined some non-mandatory parameters, break...
                if(i >= parameters.length)
                    break;
                // Non-mandatory parameter, remove the question mark...
                if(parameters[i].endsWith("?"))
                    parameters[i] = parameters[i].substring(0, parameters[i].length() - 1);

                // Variable-length parameter, remove the end
                if(parameters[i].endsWith("...")) {
                    parameters[i] = parameters[i].substring(0, parameters[i].length() - 3);
                    lpoff = i;
                }

                // First case can't happen, can it?
                if(i >= params.length)
                    paramMap.put(parameters[i], null);
                else
                    paramMap.put(parameters[i], params[i]);
            }

            // Run it...
            String lang = c.getLanguage();
            ScriptEngine se = engineManager.getEngineByExtension(lang);
            Object o = se.eval(CommandManager.findCommand("common." + lang).getScript() + c.getScript(), getContext(rto, rt, source, b, paramMap));
            if(o != null)
                doReply(con, rt, rto, o.toString());
        } catch(Throwable t) {
            doReply(con, rt, rto, "Exception caught!");
            t.printStackTrace();
        }
    }

    public void onAction(ActionEvent evt) {
    }

    public void onConnect(ConnectEvent evt) {
        SAXBuilder b = new SAXBuilder(true);
        try {
            Document d = b.build(new File("data/config.xml"));

            Element root = d.getRootElement();
            if(!root.getName().equals("config"))
                throw new RuntimeException("Bot configuration (config.xml) invalid (no root element)!");

            @SuppressWarnings("unchecked cast") List<Element> servers = root.getChildren("server");

            for(Element e : servers) {
                String host = e.getAttributeValue("host").toLowerCase();
                if(!host.equals(connection.getHost().getHostName().toLowerCase()))
                    continue;

                Element onconnect = e.getChild("onconnect");
                if(onconnect == null)
                    return;

                List children = onconnect.getChildren("cmd");
                for(Object cmd : children)
                    connection.sendRaw(((Element) cmd).getText());
                List scripts = onconnect.getChildren("loadscript");
                for(Object s : scripts) {
                    String file = ((Element) s).getText();
                    String lang = file.substring(file.lastIndexOf('.') + 1);
                    System.out.println("Loading script '" + file + "', language: " + lang);

                    ScriptEngine se = engineManager.getEngineByExtension(lang);
                    if(se == null) {
                        System.out.println("Couldn't find script engine for script: " + file);
                        return;
                    }
                    try {
                        File f = new File("data/scripts/" + file);
                        DataInputStream dis = new DataInputStream(new FileInputStream(f));
                        byte[] data = new byte[(int) f.length()];
                        dis.readFully(data);
                        dis.close();
                        se.setContext(getContext(connection.getUser().getNick(), 1, connection.getUser(), null, null));

                        Command c = CommandManager.findCommand("common." + lang);
                        if(c == null)
                            System.err.println("Don't know how to run scripts of type: " + lang);
                        else {
                            se.eval(c.getScript() + new String(data));
                        }
                    } catch(ScriptException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch(JDOMException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Bot configuration (config.xml) invalid!: " + e.getMessage());
        }
    }

    public void onCtcpRequest(CtcpRequestEvent evt) {
    }

    public void onDisconnect(DisconnectEvent evt) {
        onError(new ErrorEvent(evt.getSource(), "Internally fired so we can reconnect..."));
    }

    public void onError(ErrorEvent evt) {
        try {
            connect();
        } catch(Exception e) {
            e.printStackTrace();
            onError(evt);
        }
    }

    public void onInvite(InviteEvent evt) {
    }

    public void onJoin(JoinEvent evt) {
    }

    public void onKick(KickEvent evt) {
        if(evt.getKickedUser().equals(evt.getSource().getUser()))
            evt.getSource().join(evt.getChannel().getName());
    }

    public void onMessage(MessageEvent evt) {
        MessageTransmitter source = evt.getSender();
        if(source.getType() != MessageTransceiverType.USER)
            return;
        User u = (User) source;
        MessageReceiver target = evt.getTarget();
        String reply_to;
        int reply_type;

        if(target.getType() == MessageTransceiverType.USER) {
            reply_to = u.getNick();
            reply_type = 1;
        } else {
            reply_to = ((Channel) target).getName();
            reply_type = 0;
        }

        String message = evt.getMessage();
        if(message.startsWith(prefix)) {
            int idx = message.indexOf(' ');
            if(idx == -1)
                idx = message.length();
            String cmd = message.substring(prefix.length(), idx).toLowerCase();
            String[] params;
            if(idx == message.length())
                params = new String[0];
            else
                params = message.substring(idx + 1).split(" ");

            processCommand(evt.getSource(), cmd, params, u, reply_type, reply_to);
        }
    }

    public void onModeChange(ModeChangeEvent evt) {
    }

    public void onNickChange(NickChangeEvent evt) {
    }

    public void onNotice(NoticeEvent evt) {
    }

    public void onNumericMessage(NumericMessageEvent evt) {
    }

    public void onPart(PartEvent evt) {
    }

    public void onQuit(QuitEvent evt) {
    }

    public void onTopicChange(TopicChangeEvent evt) {
    }
}