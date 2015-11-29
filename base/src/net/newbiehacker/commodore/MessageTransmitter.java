package net.newbiehacker.commodore;

/**
 * {@code MessageTransmitter}
 *
 * @author James Lawrence
 * @version 1
 */
public interface MessageTransmitter {
    public MessageTransceiverType getType();

    public void dispose();
}