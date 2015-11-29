package net.newbiehacker.commodore;

import net.newbiehacker.commodore.event.*;
import net.newbiehacker.commodore.io.IoManager;
import net.newbiehacker.commodore.util.Constants;
import net.newbiehacker.commodore.util.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * {@code IRCConnection}
 *
 * @author James Lawrence
 * @version 1
 */
public final class IrcConnection {
    private static final Logger logger = new Logger(IrcConnection.class, Constants.DEFAULT_GLOBAL_LOGGING_LEVEL);
    private boolean connected;
    private boolean rakeInfo;
    private boolean sentConnect;
    private final ByteBuffer inBuffer;

    private InetAddress host;
    private int port;
    private final List<IrcEventListener> listeners;
    private final List<Server> serverCache;
    private final List<User> users;
    private final Map<String, Channel> channels;
    private final Map<String, String> support;
    private MessageTransmitter defTransmitter;
    private final Queue<ByteBuffer> outbound;
    private SocketChannel channel;

    private String lineBuffer;
    private String nick;

    public IrcConnection(InetAddress host, int port) {
        this.host = host;
        this.port = port;

        logger.log("IrcConnection initialised for connection to " + host.getHostName() + ":" + port);

        inBuffer = ByteBuffer.allocateDirect(1024);
        outbound = new LinkedList<ByteBuffer>();
        lineBuffer = "";
        defTransmitter = new Server(host.getHostName());
        connected = false;
        users = new ArrayList<User>();
        channels = new HashMap<String, Channel>();
        listeners = new ArrayList<IrcEventListener>();
        support = new HashMap<String, String>();
        serverCache = new ArrayList<Server>();
        serverCache.add((Server) defTransmitter);
    }

    private Channel ensureGetChannel(String chan) {
        chan = chan.toLowerCase();
        if (channels.containsKey(chan))
            return channels.get(chan);
        Channel c = new Channel(chan);
        channels.put(chan, c);
        return c;
    }

    private void fireEvent(IrcEvent e) {
        synchronized (listeners) {
            EventDispatcher.queueEvent(e, listeners.toArray(new IrcEventListener[listeners.size()]));
        }
    }

    private String from(String[] data, int off) {
        StringBuilder sb = new StringBuilder();
        for (int i = off; i < data.length; i++) {
            sb.append(data[i]);
            if (i != data.length - 1)
                sb.append(" ");
        }
        return sb.toString();
    }

    private Server getServer(String server) {
        for (Server s : serverCache) {
            if (s.getHost().equalsIgnoreCase(server))
                return s;
        }
        Server s = new Server(server);
        serverCache.add(s);
        return s;
    }

    private User ensureGetUser(String nick, String user, String host) {
        for (User u : users) {
            if (u.getNick().equalsIgnoreCase(nick)) {
                if (user != null)
                    u.user = user;
                if (host != null)
                    u.host = host;
                return u;
            }
        }
        User u = new User(nick, this);
        u.user = user;
        u.host = host;
        users.add(u);
        return u;
    }

    private void handleNumeric(int numeric, String trailing) {
        if (!sentConnect && numeric > 5) {
            fireEvent(new ConnectEvent(this));
            sentConnect = true;
        }

        String[] info = trailing.split(" ");

        switch (numeric) {
            // (KEY=VALUE|KEY)+ :are supported by this server
            case Constants.RPL_ISUPPORT:
                for (String s : info) {
                    if (s.charAt(0) == ':')
                        break;
                    if (s.indexOf('=') == -1) {
                        support.put(s, null);
                        if (s.equalsIgnoreCase("NAMESX")) {
                            sendRaw("PROTOCTL NAMESX");
                        }
                    } else {
                        String[] parts = s.split("=");
                        String key = parts[0];
                        String value = parts[1];
                        support.put(key, value);
                    }
                }
                break;

            // [chan] modes...
            case Constants.RPL_CHANNELMODEIS:
                Channel chan = ensureGetChannel(info[0]);
                boolean add = true;
                String[] parts = from(info, 1).split(" ");
                if (parts[0].charAt(0) == ':')
                    parts[0] = parts[0].substring(1);
                char[] chars = parts[0].toCharArray();
                int poff = 1;
                for (char ch : chars) {
                    if (ch == '+') {
                        add = true;
                        continue;
                    } else if (ch == '-') {
                        add = false;
                        continue;
                    }

                    String chanmodes = "beI,kfL,lj,psmntirRcOAQKVCuzNSMTG";
                    if (supports("CHANMODES"))
                        chanmodes = getSupportInfo("CHANMODES");
                    String[] groups = chanmodes.split(",");
                    int group = -1;
                    for (int i = 0; i < groups.length; i++)
                        if (groups[i].indexOf(ch) != -1) {
                            group = i;
                            break;
                        }
                    if (group == -1)
                        logger.warn("Unknown mode character: " + ch + " on channel!");
                    else {
                        switch (group) {
                            case 1:
                                if (add) {
                                    chan.modes.put(ch, parts[poff++]);
                                } else {
                                    chan.modes.remove(ch);
                                    ++poff; // It'll still be there :O
                                }
                                break;
                            case 2:
                                if (add)
                                    chan.modes.put(ch, parts[poff++]);
                                else
                                    chan.modes.remove(ch);
                                break;
                            case 3:
                                if (add)
                                    chan.modes.put(ch, null);
                                else
                                    chan.modes.remove(ch);
                                break;
                        }
                    }
                }
                break;

            // [chan] creation time
            case Constants.RPL_CHANNELCREATED:
                Channel c = ensureGetChannel(info[0]);
                c.creationTime = new Date(Long.parseLong(info[1]) * 100);
                break;

            // [chan] :TOPIC...
            case Constants.RPL_TOPIC:
                c = ensureGetChannel(info[0]);
                c.topic = from(info, 1).substring(1);
                break;

            // [chan] [nick] [time]
            case Constants.RPL_TOPICINFO:
                c = ensureGetChannel(info[0]);
                String nick = info[1];
                long time = Long.parseLong(info[2]);
                c.topicSetter = nick;
                c.topicSetTime = new Date(time * 1000);
                break;

            // [chan] [user] [host] [server] [nick] [modes] :[hops] real name...
            case Constants.RPL_WHOREPLY:
                c = ensureGetChannel(info[0]);
                String user = info[1];
                String host = info[2];
                nick = info[4];
                String modes = info[5];
                String realName = from(info, 7);

                User u = ensureGetUser(nick, user, host);
                u.real = realName;

                String prefix = "(qahov)~&%@+";
                if (supports("PREFIX"))
                    prefix = getSupportInfo("PREFIX");

                String[] pparts = prefix.split("\\)");
                String m = pparts[0].substring(1);
                String p = pparts[1];

                int idx;

                for (char c1 : modes.toCharArray()) {
                    if ((idx = p.indexOf(c1)) != -1) {
                        if (!c.rights.containsKey(u))
                            c.rights.put(u, new ArrayList<Character>());
                        char c2 = m.charAt(idx);
                        if (!c.rights.get(u).contains(c2))
                            c.rights.get(u).add(c2);
                    } else if (!u.modes.contains(c1))
                        u.modes.add(c1);
                }
                break;

            // = [chan] :(~&@%+)*[nick]...
            case Constants.RPL_NAMEREPLY:
                String s = info[1].toLowerCase();
                c = ensureGetChannel(s);
                if (info[2].charAt(0) == ':')
                    info[2] = info[2].substring(1);
                for (int i = 2; i < info.length; i++) {
                    String inf = info[i];

                    prefix = "(qahov)~&@%+";
                    if (supports("PREFIX"))
                        prefix = getSupportInfo("PREFIX");
                    String[] bits = prefix.split("\\)");
                    String mode = bits[0].substring(1);
                    String pre = bits[1];

                    while (pre.indexOf(info[i].charAt(0)) != -1)
                        info[i] = info[i].substring(1);

                    u = ensureGetUser(info[i], null, null);
                    if(rakeInfo)
                        sendRaw("WHOIS " + info[i]);
                    if (!c.users.contains(u))
                        c.users.add(u);
                    if (!u.channels.containsKey(s))
                        u.channels.put(s, c);

                    while ((idx = pre.indexOf(inf.charAt(0))) != -1) {
                        if (!c.rights.containsKey(u))
                            c.rights.put(u, new ArrayList<Character>());
                        if (!c.rights.get(u).contains(mode.charAt(idx)))
                            c.rights.get(u).add(mode.charAt(idx));
                        inf = inf.substring(1);
                    }
                }
                break;

            // [nick] :is a registered nick
            case Constants.RPL_WHOISIDENTIFIED:
                ensureGetUser(info[0], null, null).identified = true;
                break;

            // [nick] [idlesecs] [signontime] :seconds idle, signon time
            case Constants.RPL_WHOISIDLE:
                u = ensureGetUser(info[0], null, null);

                int secs = Integer.parseInt(info[1]);
                int signontime = Integer.parseInt(info[2]);

                u.idleSince = new Date(new Date().getTime() - (secs * 1000));
                u.signonTime = new Date((long) signontime * 1000L);
                break;
        }
    }

    private boolean isChannel(String s) {
        String chanPrefix = getSupportInfo("CHANTYPES");
        if (chanPrefix == null)
            chanPrefix = "#&!+";
        return chanPrefix.indexOf(s.charAt(0)) != -1;
    }

    private void parse(String line) {
        String[] parts = line.split(" ");

        // Fail fix
        if (parts.length == 1)
            return;

        int off = 0;

        MessageTransmitter source = defTransmitter;

        // Who's the message from...
        if (parts[off].charAt(0) == ':') {
            String s = parts[off].substring(1);
            int usidx, hsidx;
            // Message received from another user
            if ((usidx = s.indexOf('!')) != -1 && (hsidx = s.indexOf('@')) > usidx) {
                String nick = s.substring(0, usidx);
                String user = s.substring(usidx + 1, hsidx);
                String host = s.substring(hsidx + 1);
                source = ensureGetUser(nick, user, host);
                ((User) source).idleSince = new Date();
            } else {
                source = getServer(s);
            }
            off++;
        }

        if (parts[off].matches("\\d\\d\\d")) {
            int numeric = Integer.parseInt(parts[off]);
            User u = ensureGetUser(parts[++off], null, null);

            int trail_off = 0;
            for (int i = off; i >= 0; i--)
                trail_off += parts[i].length() + 1;

            String trailing = "";
            if (trail_off < line.length())
                trailing = line.substring(trail_off);
            handleNumeric(numeric, trailing);
            fireEvent(new NumericMessageEvent(this, source, u, numeric, trailing));
            return;
        }

        if (parts[off].equalsIgnoreCase("PING")) {
            String resp = parts[off + 1];
            if (resp.charAt(0) == ':')
                resp = resp.substring(1);
            sendRaw("PONG :" + resp);
        } else if (parts[off].equalsIgnoreCase("NOTICE")) {
            String target = parts[++off];
            MessageReceiver t = isChannel(target) ? ensureGetChannel(target) : ensureGetUser(target, null, null);
            fireEvent(new NoticeEvent(this, source, t, from(parts, off + 1).substring(1)));
        } else if (parts[off].equalsIgnoreCase("PRIVMSG")) {
            String target = parts[++off];
            MessageReceiver t = isChannel(target) ? ensureGetChannel(target) : ensureGetUser(target, null, null);
            String message = from(parts, off + 1).substring(1);
            if (message.length() >= 1 && message.charAt(0) == 1 && message.charAt(message.length() - 1) == 1) {
                if (message.length() > 8 && message.substring(1, 7).equalsIgnoreCase("ACTION"))
                    fireEvent(new ActionEvent(this, source, t, message.substring(8, message.length() - 1)));
                else
                    fireEvent(new CtcpRequestEvent(this, source, t, message.substring(1, message.length() - 1)));
            } else
                fireEvent(new MessageEvent(this, source, t, message));
        } else if (parts[off].equalsIgnoreCase("JOIN")) {
            Channel c = ensureGetChannel(parts[++off].substring(1));
            User u = (User) source;
            if (!c.users.contains(u))
                c.users.add(u);
            if (!u.channels.containsValue(c))
                u.channels.put(c.getName().toLowerCase(), c);
            if (rakeInfo) {
                if (u.nick.equalsIgnoreCase(nick)) {
                    sendRaw("MODE " + c.getName());
                    sendRaw("WHO " + c.getName());
                } else {
                    sendRaw("WHO " + u.getNick());
                    sendRaw("WHOIS " + u.getNick());
                }
            }
            fireEvent(new JoinEvent(this, c, (User) source));
        } else if (parts[off].equalsIgnoreCase("ERROR")) {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fireEvent(new ErrorEvent(this, from(parts, off + 1).substring(1)));
        } else if (parts[off].equalsIgnoreCase("PART")) {
            Channel c = ensureGetChannel(parts[++off]);
            User u = (User) source;
            c.users.remove(u);
            u.channels.remove(c.getName().toLowerCase());
            String message = null;
            if (off + 1 < parts.length)
                message = from(parts, off + 1).substring(1);
            fireEvent(new PartEvent(this, c, (User) source, message));
        } else if (parts[off].equalsIgnoreCase("QUIT")) {
            String message = null;
            if (off + 1 < parts.length)
                message = from(parts, off + 1).substring(1);
            fireEvent(new QuitEvent(this, (User) source, message));
            User u = (User) source;
            u.identified = false;
            for (Channel c : channels.values()) {
                c.users.remove(u);
                u.channels.remove(c.getName().toLowerCase());
            }
        } else if (parts[off].equalsIgnoreCase("NICK")) {
            User u = (User) source;
            String oldNick = u.getNick();
            String newNick = parts[off + 1];
            if (newNick.charAt(0) == ':')
                newNick = newNick.substring(1);
            u.nick = newNick;
            if (oldNick.equals(nick))
                nick = newNick;
            u.identified = false;
            fireEvent(new NickChangeEvent(this, oldNick, u));
        } else if (parts[off].equalsIgnoreCase("MODE")) {
            String target = parts[++off];
            MessageReceiver t = isChannel(target) ? ensureGetChannel(target) : ensureGetUser(target, null, null);

            // Parse mode string...
            boolean add = true;
            if (parts[++off].charAt(0) == ':')
                parts[off] = parts[off].substring(1);
            char[] chars = parts[off++].toCharArray();
            int poff = off;
            boolean channel = t.getClass() == Channel.class;
            for (char c : chars) {
                if (c == '+') {
                    add = true;
                    continue;
                } else if (c == '-') {
                    add = false;
                    continue;
                }
                if (channel) {
                    Channel chan = (Channel) t;
                    // First check for rights change
                    String prefix = "(qahov)~&@%+";
                    if (supports("PREFIX"))
                        prefix = getSupportInfo("PREFIX");
                    String mode = prefix.split("\\)")[0].substring(1);
                    if (mode.indexOf(c) != -1) {
                        User u = ensureGetUser(parts[poff++], null, null);
                        if (add) {
                            if (!chan.rights.containsKey(u))
                                chan.rights.put(u, new ArrayList<Character>());
                            if (!chan.rights.get(u).contains(c))
                                chan.rights.get(u).add(c);
                        } else {
                            if (chan.rights.containsKey(u))
                                chan.rights.get(u).remove((Character) c);
                        }
                        fireEvent(new ModeChangeEvent(this, source, t, String.valueOf(add ? '+' : '-') +
                                String.valueOf(c), parts[poff - 1]));
                        continue;
                    }

                    String chanmodes = "beI,kfL,lj,psmntirRcOAQKVCuzNSMTG";
                    if (supports("CHANMODES"))
                        chanmodes = getSupportInfo("CHANMODES");
                    String[] groups = chanmodes.split(",");
                    int group = -1;
                    for (int i = 0; i < groups.length; i++)
                        if (groups[i].indexOf(c) != -1) {
                            group = i;
                            break;
                        }
                    if (group == -1)
                        logger.warn("Unknown mode character: " + c + " on channel!");
                    else {
                        switch (group) {
                            case 0:
                                String param = parts[poff++];
                                if (add) {
                                    if (!chan.lists.containsKey(c))
                                        chan.lists.put(c, new ArrayList<ListEntry>());
                                    chan.lists.get(c).add(new ListEntry(source.toString(), param, new Date()));
                                } else {
                                    if (chan.lists.containsKey(c)) {
                                        for (ListEntry le : chan.lists.get(c)) {
                                            if (le.getMask().equalsIgnoreCase(param)) {
                                                chan.lists.get(c).remove(le);
                                                break;
                                            }
                                        }
                                    }
                                }
                                fireEvent(new ModeChangeEvent(this, source, t, String.valueOf(add ? '+' : '-') +
                                        String.valueOf(c), param));
                                break;
                            case 1:
                                param = parts[poff++];
                                if (add) {
                                    chan.modes.put(c, param);
                                } else {
                                    chan.modes.remove(c);
                                }
                                fireEvent(new ModeChangeEvent(this, source, t, String.valueOf(add ? '+' : '-') +
                                        String.valueOf(c), param));
                                break;
                            case 2:
                                if (add)
                                    chan.modes.put(c, parts[poff++]);
                                else
                                    chan.modes.remove(c);
                                fireEvent(new ModeChangeEvent(this, source, t, String.valueOf(add ? '+' : '-') +
                                        String.valueOf(c), add ? parts[poff - 1] : null));
                                break;
                            case 3:
                                if (add)
                                    chan.modes.put(c, null);
                                else
                                    chan.modes.remove(c);
                                fireEvent(new ModeChangeEvent(this, source, t, String.valueOf(c), null));
                                break;
                        }
                    }
                } else {
                    User u = (User) t;
                    if (add)
                        u.modes.add(c);
                    else
                        u.modes.remove((Character) c);
                    fireEvent(new ModeChangeEvent(this, source, t, String.valueOf(add ? '+' : '-') +
                            String.valueOf(c), null));
                }
            }
        } else if (parts[off].equalsIgnoreCase("TOPIC")) {
            Channel c = ensureGetChannel(parts[++off]);
            c.topic = from(parts, off + 1).substring(1);
            c.topicSetter = ((User) source).getNick();
            c.topicSetTime = new Date();
            fireEvent(new TopicChangeEvent(this, c));
        } else if (parts[off].equalsIgnoreCase("KICK")) {
            Channel c = ensureGetChannel(parts[++off]);
            User u = ensureGetUser(parts[++off], null, null);
            String message = null;
            if (parts.length > off + 1) {
                message = from(parts, off + 1);
                if (message.charAt(0) == ':')
                    message = message.substring(1);
            }
            fireEvent(new KickEvent(this, source, c, u, message));
        } else if (parts[off].equalsIgnoreCase("INVITE")) {
            User u = ensureGetUser(parts[++off], null, null);
            String chan = parts[++off];
            if (chan.charAt(0) == ':')
                chan = chan.substring(1);
            Channel c = getChannel(chan);
            fireEvent(new InviteEvent(this, source, u, c));
        } else {
            logger.warn("Unhandled command: " + parts[off]);
        }
    }

    private void sendEnsureLength(String prefix, String message, String suffix) {
        int len = prefix.length();

        // Message length includes the prefix
        int max_msg_len = Constants.MESSAGE_SPLIT_LENGTH - len;

        // If we have a suffix, message length includes that
        if (suffix != null)
            max_msg_len -= suffix.length();

        // While the message is too long to send in one chunk, split it up
        while (message.length() >= max_msg_len) {
            // Find the last space in the maximum message length (if any)
            int idx = message.substring(0, max_msg_len).lastIndexOf(' ');
            // If no space, we'll just have to truncate the word
            if (idx == -1)
                idx = max_msg_len;

            // Split the message
            String sub = message.substring(0, idx);
            // Remove the sub messge from the message
            message = message.substring(message.charAt(idx) == ' ' ? idx + 1 : idx);

            // Send the sub chunk
            sendRaw(prefix + sub + (suffix != null ? suffix : ""));
        }

        // If the message still has some data left (it could have ended with a space, bizarrely)
        if (message.length() > 1)
            sendRaw(prefix + message + (suffix != null ? suffix : ""));
    }

    public IrcEventListener addListener(IrcEventListener iel) {
        synchronized (listeners) {
            listeners.add(iel);
        }
        return iel;
    }

    public List<IrcEventListener> getListeners() {
        synchronized (listeners) {
            return Collections.unmodifiableList(listeners);
        }
    }

    public void changeNick(String newNick) {
        sendRaw("NICK " + newNick);
    }

    public void close() throws IOException {
        channel.close();
        inBuffer.clear();
        outbound.clear();
        lineBuffer = "";
        for (User u : users)
            u.dispose();
        users.clear();
        for (Channel c : channels.values())
            c.dispose();
        sentConnect = false;
        channels.clear();
        connected = false;
        fireEvent(new DisconnectEvent(this));
    }

    public void connect(String nick, String user, String realname) throws IOException {
        sentConnect = false;
        connected = false;
        this.nick = nick;
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        IoManager.registerConnection(this, sc);

        InetSocketAddress a = new InetSocketAddress(host, port);
        sc.connect(a);

        channel = sc;
        sendRaw("NICK " + nick);
        sendRaw("USER " + user + " * * :" + realname);
        connected = true;
    }

    public Channel getChannel(String channel) {
        return channels.get(channel);
    }

    public boolean onChannel(String channel) {
        return channels.containsKey(channel);
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSupportInfo(String key) {
        return support.get(key);
    }

    public User getUser(String nick) {
        for (User u : users)
            if (u.getNick().equalsIgnoreCase(nick))
                return u;
        return null;
    }

    public User getUser() {
        return ensureGetUser(nick, null, null);
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public boolean isConnected() {
        return connected;
    }

    public void join(String channel) {
        sendRaw("JOIN " + channel);
    }

    public void part(String channel, String reason) {
        sendRaw("PART " + channel + (reason != null ? " :" + reason : ""));
    }

    public void part(String channel) {
        part(channel, null);
    }

    public void quit(String reason) {
        sendRaw("QUIT" + (reason != null ? " :" + reason : ""));
    }

    public void quit() {
        quit(null);
    }

    public boolean rakingInfo() {
        return rakeInfo;
    }

    public void read() throws IOException {
        int read = channel.read(inBuffer);

        if (read == -1) {
            close();
            IoManager.unregisterConnection(this);
            return;
        }

        inBuffer.flip();

        byte[] data = new byte[read];
        inBuffer.get(data, 0, read);
        inBuffer.clear();


        // Append to our line buffer until we find a line...
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n' || (data[i] == '\r' && i + 1 < data.length && data[i + 1] == '\n') || data[i] == '\r') {
                if (data[i] == '\r' && i + 1 < data.length && data[i + 1] == '\n')
                    ++i;
                logger.verbose("<<< " + lineBuffer);
                parse(lineBuffer);
                lineBuffer = "";
            } else
                lineBuffer += (char) data[i];
        }
    }

    public void removeListener(IrcEventListener iel) {
        synchronized (listeners) {
            listeners.remove(iel);
        }
    }

    public void sendAction(String recipient, String action) {
        sendEnsureLength("PRIVMSG " + recipient + " \u0001ACTION ", action, "\u0001");
    }

    public void sendCtcpRequest(String recipient, String ctcp) {
        sendEnsureLength("PRIVMSG " + recipient + " \u0001", ctcp, "\u0001");
    }

    public void sendCtcpResponse(String recipient, String resp) {
        sendEnsureLength("NOTICE " + recipient + " \u0001", resp, "\u0001");
    }

    public void sendMessage(String recipient, String message) {
        sendEnsureLength("PRIVMSG " + recipient + " :", message, null);
    }

    public void sendNotice(String recipient, String message) {
        sendEnsureLength("NOTICE " + recipient + " :", message, null);
    }

    public void sendModeChange(String target, String modeString) {
        sendRaw("MODE " + target + " " + modeString);
    }

    public void sendRaw(String line) {
        synchronized (outbound) {
            outbound.add(ByteBuffer.wrap((line + "\r\n").getBytes()));
        }
    }

    public void setRakingInfo(boolean b) {
        rakeInfo = b;
    }

    public void setLoggingLevel(Set<Logger.Level> levels) {
        logger.setLoggingLevel(levels);
    }

    public boolean supports(String s) {
        return support.containsKey(s);
    }

    public void write() throws Throwable {
        if (outbound.size() == 0)
            return;

        ByteBuffer b;
        synchronized (outbound) {
            b = outbound.peek();
        }

        channel.write(b);

        if (b.remaining() == 0) {
            if (logger.getLoggingLevel().contains(Logger.Level.VERBOSE)) {
                byte[] data = b.array();
                logger.verbose(">>> " + new String(data).replace("\r", "").replace("\n", ""));
            }
            outbound.poll();
        }
    }
}