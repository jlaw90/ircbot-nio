package net.newbiehacker.commodore.event;

import net.newbiehacker.commodore.util.Constants;
import net.newbiehacker.commodore.util.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The {@code EventDispatcher} dispatches events to event listeners on multiple threads to prevent deadlock from listeners
 *
 * @author James Lawrence
 * @version 1
 */
public final class EventDispatcher implements Runnable {
    private static final Logger logger = new Logger(EventDispatcher.class, Constants.DEFAULT_GLOBAL_LOGGING_LEVEL);

    private static final Object lock;
    private static final Queue<QueuedEvent> queue;

    /**
     * Queues an event for dispatch by a dispatcher thread<br/>
     * {@code targets} is used so that we don't have to synchronise on the connections listeners, and also it means
     * listeners added after this event was fired but before it was dispatched won't be notified
     *
     * @param e       the event to be dispatched
     * @param targets the listeners that will be notified of this event
     */
    public static void queueEvent(IrcEvent e, IrcEventListener[] targets) {
        synchronized (lock) {
            queue.add(new QueuedEvent(e, targets));
        }
    }

    static {
        lock = new Object();
        queue = new LinkedList<QueuedEvent>();

        for (int i = 0; i < Constants.EVENT_DISPATCHER_THREADS; i++) {
            Thread t = new Thread(new EventDispatcher(), "Event-Dispatcher-" + i);
            t.setDaemon(true);
            t.setPriority(Constants.EVENT_DISPATCHER_THREAD_PRIORITY);
            t.start();
        }
    }

    private static class QueuedEvent {
        private IrcEvent evt;
        private IrcEventListener[] targets;

        public QueuedEvent(IrcEvent evt, IrcEventListener[] targets) {
            this.evt = evt;
            this.targets = targets;
        }

        public void dispose() {
            evt = null;
            targets = null;
        }
    }

    private EventDispatcher() {
    }

    public void run() {
        int available;
        QueuedEvent evt = null;
        while (true) {
            try {
                synchronized (lock) {
                    available = queue.size();
                    if (available != 0)
                        evt = queue.poll();
                }
                if (available != 0) {
                    IrcEvent e = evt.evt;
                    Class c = e.getClass();
                    // Someone please god find me a better way to do this...
                    for (IrcEventListener t : evt.targets) {
                        try {
                            if (c == ConnectEvent.class)
                                t.onConnect((ConnectEvent) e);
                            else if (c == NoticeEvent.class)
                                t.onNotice((NoticeEvent) e);
                            else if (c == NumericMessageEvent.class)
                                t.onNumericMessage((NumericMessageEvent) e);
                            else if (c == MessageEvent.class)
                                t.onMessage((MessageEvent) e);
                            else if (c == ActionEvent.class)
                                t.onAction((ActionEvent) e);
                            else if (c == DisconnectEvent.class)
                                t.onDisconnect((DisconnectEvent) e);
                            else if (c == ErrorEvent.class)
                                t.onError((ErrorEvent) e);
                            else if (c == JoinEvent.class)
                                t.onJoin((JoinEvent) e);
                            else if (c == NickChangeEvent.class)
                                t.onNickChange((NickChangeEvent) e);
                            else if (c == PartEvent.class)
                                t.onPart((PartEvent) e);
                            else if (c == QuitEvent.class)
                                t.onQuit((QuitEvent) e);
                            else if (c == ModeChangeEvent.class)
                                t.onModeChange((ModeChangeEvent) e);
                            else if (c == TopicChangeEvent.class)
                                t.onTopicChange((TopicChangeEvent) e);
                            else if(c == CtcpRequestEvent.class)
                                t.onCtcpRequest((CtcpRequestEvent) e);
                            else if(c == InviteEvent.class)
                                t.onInvite((InviteEvent) e);
                            else if(c == KickEvent.class)
                                t.onKick((KickEvent) e);
                            else {
                                logger.warn("Undispatched (unknown) event in dispatch thread.");
                                logger.warn(e);
                            }
                        } catch (Throwable t1) {
                            t1.printStackTrace();
                        }
                    }
                    evt.dispose();
                }
                Thread.sleep(Constants.EVENT_DISPATCHER_THREAD_SLEEP_DURATION);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}