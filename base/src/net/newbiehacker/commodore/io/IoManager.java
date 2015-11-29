package net.newbiehacker.commodore.io;

import net.newbiehacker.commodore.IrcConnection;
import net.newbiehacker.commodore.util.Constants;
import net.newbiehacker.commodore.util.Logger;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * {@code IOManager}
 *
 * @author James Lawrence
 * @version 1
 */
public final class IoManager implements Runnable {
    private static final Logger logger = new Logger(IoManager.class, Constants.DEFAULT_GLOBAL_LOGGING_LEVEL);
    private static final Map<SelectionKey, IrcConnection> map;
    private static final Selector selector;

    public static void registerConnection(IrcConnection c, SocketChannel channel) throws ClosedChannelException {
        synchronized (selector) {
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            map.put(key, c);
        }
        logger.debug("Connection registered!");
    }

    public static void unregisterConnection(IrcConnection c) {
        synchronized(selector) {
            if(map.containsValue(c)) {
                for (Map.Entry<SelectionKey, IrcConnection> e : map.entrySet()) {
                    if (e.getValue() == c) {
                        map.remove(e.getKey());
                        return;
                    }
                }
            }
        }
    }

    public static Collection<IrcConnection> getConnections() {
        return Collections.unmodifiableCollection(map.values());
    }

    static {
        map = new HashMap<SelectionKey, IrcConnection>();
        try {
            selector = SelectorProvider.provider().openSelector();
        } catch (IOException e) {
            throw new RuntimeException("Could not create selector for IO", e);
        }

        Thread t = new Thread(new IoManager(), "Io");
        t.setPriority(Constants.IO_THREAD_PRIORITY);
        t.start();
    }

    private IoManager() {

    }

    public void run() {
        while (true) {
            try {
                synchronized (selector) {
                    if (selector.selectNow() != 0) {
                        Set<SelectionKey> keys = selector.selectedKeys();

                        Iterator<SelectionKey> it = keys.iterator();

                        while (it.hasNext()) {
                            SelectionKey k = it.next();
                            it.remove();

                            if(!k.isValid())
                                continue;

                            IrcConnection ic = map.get(k);
                            if (ic == null) {
                                logger.warn("No connection found for selection key, being removed from selector!");
                                k.cancel();
                                continue;
                            }

                            try {
                                if(k.isConnectable()) {
                                    ((SocketChannel) k.channel()).finishConnect();
                                }
                                if (k.isWritable()) {
                                    ic.write();
                                }
                                if (k.isReadable()) {
                                    ic.read();
                                }
                            } catch (Throwable t) {
                                k.cancel();
                                ic.close();
                                t.printStackTrace();
                            }
                        }
                    }
                    Thread.sleep(Constants.IO_THREAD_SLEEP_DURATION);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}