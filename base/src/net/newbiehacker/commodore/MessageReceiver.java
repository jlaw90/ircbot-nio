package net.newbiehacker.commodore;

/**
 * {@code MessageReceiver}
 *
 * @author James Lawrence
 * @version 1
 */
public interface MessageReceiver {
    public MessageTransceiverType getType();

    public void dispose();
}