package net.newbiehacker.commodore.util;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

/**
 * {@code Logger}
 *
 * @author James Lawrence
 * @version 1
 */
public final class Logger {
    public enum Level {
        ERROR,
        WARN,
        NORM,
        VERBOSE,
        DEBUG
    }

    public static final Set<Level> DEFAULT = Collections.unmodifiableSet(EnumSet.of(Level.ERROR, Level.WARN, Level.NORM));
    public static final Set<Level> ALL = Collections.unmodifiableSet(EnumSet.allOf(Level.class));

    private static final SimpleDateFormat format;

    static {
        format = new SimpleDateFormat("hh:mm:ss ");
    }

    private Set<Level> logging_level;
    private String owner;

    public Logger(Class owner, Set<Level> logging_level) {
        this.owner = owner.getCanonicalName();
        this.logging_level = EnumSet.copyOf(logging_level);
    }

    public Logger(Class owner) {
        this(owner, DEFAULT);
    }

    public Set<Level> getLoggingLevel() {
        return Collections.unmodifiableSet(logging_level);
    }

    public void setLoggingLevel(Set<Level> logging_level) {
        this.logging_level = EnumSet.copyOf(logging_level);
    }

    public void log(Object message, Level level) {
        if(logging_level.contains(level))
            System.out.println(format.format(new Date()) + "[" + owner + "]# " + message);
    }

    public void err(Object message) {
        log(message, Level.ERROR);
    }

    public void warn(Object message) {
        log(message, Level.WARN);
    }

    public void log(Object message) {
        log(message, Level.NORM);
    }

    public void verbose(Object message) {
        log(message, Level.VERBOSE);
    }

    public void debug(Object message) {
        log(message, Level.DEBUG);
    }
}