import 'net.newbiehacker.commodore.event.IrcEventListener'

module MoparscapeHelper
  class Listener
      include IrcEventListener
    def onAction(evt)
    end

    def onConnect(evt)
    end

    def onCtcpRequest(evt)
    end

    def onDisconnect(evt)
    end

    def onError(evt)
    end

    def onInvite(evt)
    end

    def onJoin(evt)
      src = evt.getSource
      chan = evt.getChannel
      user = evt.getUser

      if chan.getName == '#moparscape'
        src.sendNotice user.getNick, 'Hi ' + user.getNick + ', please make sure you check the following page before asking for help, thanks! http://cakenet.net/moparscape.help.htm'
      end
    end

    def onKick(evt)
    end

    def onMessage(evt)
    end

    def onModeChange(evt)
    end

    def onNickChange(evt)
    end

    def onNotice(evt)
    end

    def onNumericMessage(evt)
    end

    def onPart(evt)
    end

    def onQuit(evt)
    end

    def onTopicChange(evt)
    end
  end
end

$irc.removeListener($mshelplistener) if !$mshelplistener.nil?
$mshelplistener = MoparscapeHelper::Listener.new
$irc.addListener($mshelplistener)