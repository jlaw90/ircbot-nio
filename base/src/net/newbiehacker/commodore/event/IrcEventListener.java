package net.newbiehacker.commodore.event;


/**
 * {@code IrcEventListener}s are notified of events that happen on any connections that they have registered with for
 * notifications.
 * <b>Important:</b> implmenetations of this interface should be aware that multiple methods may be called at the same
 * time as the event dispatcher has multiple dispatching threads, so make sure to write thread-safe code for any collections!
 *
 * @author James Lawrence
 * @version 1
 */
public interface IrcEventListener {
    /**
     * This is called when we receive an action from a user or a channel
     * @param evt the event
     */
    public void onAction(ActionEvent evt);

    /**
     * This is called when a connection is fully connected<br/>
     * After receiving this notification, you should be able to join channels, etc.
     * @param evt the event
     */
    public void onConnect(ConnectEvent evt);

    /**
     * This is called when we receive a ctcp request from something
     * @param evt the event
     */
    public void onCtcpRequest(CtcpRequestEvent evt);

    /**
     * This is called when a connection is disconnected
     * @param evt the event
     */
    public void onDisconnect(DisconnectEvent evt);

    /**
     * This is called when we receive an error message from the server
     * @param evt the event
     */
    public void onError(ErrorEvent evt);

    /**
     * This is called when we receive an invite from a user to a channel
     * @param evt the event
     */
    public void onInvite(InviteEvent evt);

    /**
     * This is called when a user joins a channel we are on
     * @param evt the event
     */
    public void onJoin(JoinEvent evt);

    /**
     * This is called when a user is kicked from a channel
     * @param evt the event
     */
    public void onKick(KickEvent evt);

    /**
     * This is called when we receive a message from a user or a channel
     * @param evt the event
     */
    public void onMessage(MessageEvent evt);

    /**
     * This is called when the modes of a channel or a user are changed
     * @param evt the event
     */
    public void onModeChange(ModeChangeEvent evt);

    /**
     * This is called when a user changes their nickname
     * @param evt the event
     */
    public void onNickChange(NickChangeEvent evt);

    /**
     * This is called when we receive a notice from a user or a channel
     * @param evt the event
     */
    public void onNotice(NoticeEvent evt);

    /**
     * This is called when we receive a numeric message from the server
     * @param evt the event
     */
    public void onNumericMessage(NumericMessageEvent evt);

    /**
     * This is called when a user parts from a channel
     * @param evt the event
     */
    public void onPart(PartEvent evt);

    /**
     * This is called when a user disconnects from the server
     * @param evt the event
     */
    public void onQuit(QuitEvent evt);

    /**
     * This is called when the topic on a channel is changed
     * @param evt the event
     */
    public void onTopicChange(TopicChangeEvent evt);
}