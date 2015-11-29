module QuoteDatabase
  require 'yaml'

  import 'net.newbiehacker.commodore.event.IrcEventListener'
  import 'net.newbiehacker.commodore.MessageTransceiverType'
  import 'net.newbiehacker.bot.UserManager'
  import 'net.newbiehacker.bot.GroupManager'

  class Quote
    attr_accessor :quote, :contributer, :server, :channel, :quoter, :time, :vplus, :vminus, :voters

    def initialize(quoter, quote, contributer, server, channel)
      @quote = quote
      @channel = channel
      @contributer = contributer
      @server = server
      @quoter = quoter
      @time = Time.now
      @vplus = 0
      @vminus = 0
      @voters = Array.new
    end
  end

  class Listener
    include IrcEventListener

    attr_accessor :qdb

    def initialize
      @qdb = Array.new
      @qdb = YAML.load_file('data/qdb.yaml') if(File.exist?('data/qdb.yaml'))

      # Saved messages from channels...
      @lastmsg = {}
      @lastmsgtime = {}
      @lastmsgsnd = {}
    end

    def save
      File.open('data/qdb.yaml', 'w') do |out|
        YAML.dump(@qdb , out)
      end
    end

    def on_action(evt)
    end

    def on_connect(evt)
    end

    def on_ctcp_request(evt)
    end

    def on_disconnect(evt)
    end

    def on_error(evt)
    end

    def on_invite(evt)
    end

    def on_join(evt)
    end

    def on_kick(evt)
    end

    def on_message(evt)
      src = evt.getSource
      snd = evt.getSender
      tgt = evt.getTarget
      msg = evt.getMessage
      bu = UserManager.getUser(snd)
      if(bu == nil || tgt.getType() != MessageTransceiverType::CHANNEL)
        return
      end
      split = msg.split(' ')
      cmd = split[0]
      params = Array.new
      params = split[1..-1] if split.length > 1
      plen = params.length

      # Kill qdb script
      if cmd == 'qkill' and GroupManager.inGroup(bu, 'admin')
        src.removeListener(self)
        src.sendMessage(tgt.to_s, 'QDB listener removed.')

      # Delete a quote
      elsif cmd == 'qdel'
        if(plen < 1)
          src.sendMessage(tgt.to_s, 'Usage: qdel [Quote ID]');
        else
          i = params[0].to_i
          q = @qdb[i]
          if q != nil and (q.contributer == snd or GroupManager.inGroup(bu, 'admin'))
            @qdb[i] = nil
            save
            src.sendMessage(tgt.to_s, 'Quote ' + i.to_s + ' deleted.');
          end
        end

      # Add previous sentence to qdb
      elsif cmd == 'qadd'
        # Find a place in the qdb array if there's a slot...
        return if @lastmsg[tgt].nil?
        if @lastmsgsnd[tgt] == snd
          src.sendMessage(tgt.to_s, 'You can\'t quote yourself, douchebag...')
          return
        end
        di = -1
        @qdb.each_with_index {|v, i| di = i if v == nil and di == -1}

        n = Quote.new(@lastmsgsnd[tgt].getNick(), @lastmsg[tgt], snd.getNick(), src.getHost().getHostName(), tgt.to_s)
        if di != -1
          @qdb[di] = n
        else
          @qdb << n
          di = @qdb.length - 1
        end
        save
        src.sendMessage(tgt.to_s, 'Quote inserted into database, qid #' + di.to_s);

      # Return the specified quote
      elsif cmd == 'qget'
        if(plen < 1)
          src.sendMessage(tgt.to_s, 'Usage: qget [Quote ID]');
        else
          q = @qdb[params[0].to_i]
          if q != nil
            src.sendMessage(tgt.to_s, snd.getNick() + ', ' + q.quoter + ' said \'' + q.quote + '\', please type \'qinf ' + params[0] + '\' for more info');
          end
        end

      # Return more information about the specified quote
      elsif cmd == 'qinf'
        if(plen < 1)
          src.sendMessage(tgt.to_s, 'Usage: qinf [Quote ID]');
        else
          q = @qdb[params[0].to_i]
          return if q.nil?
          str = snd.getNick() + ', Quote was recorded by ' + q.contributer
          str += ' on ' + q.time.strftime('%a %d %b at %H:%M')
          str += ' on channel ' + q.channel + ', network was ' + q.server + '.';

          # Voting stuff...
          u = q.vplus
          d = q.vminus
          if(u > d)
          # Stop division by 0 :O
            idx = ((d.to_f / u.to_f) * 10).to_i / 2;
            strs = ['the best quote ever', 'excellent', 'amazing', 'very good', 'good']
            qstr = strs[idx]
            str += ' Votes indicate that this quote is ' + qstr;
          elsif(d > u)
            idx = ((u.to_f / d.to_f) * 10).to_i / 2;
            strs = ['the worst quote in history', 'amazingly bad', 'awful', 'very poor', 'poor']
            qstr = strs[idx]
            str += ' Unfortunately, according to votes, this quote is ' + qstr;
          else
            str += ' This quote has a neutral vote ranking so far'
          end
          str += '. Vote for this quote by using qup or qdown!'

          src.sendMessage(tgt.to_s, str);
        end
      elsif cmd == 'qup'
        if(plen < 1)
          src.sendMessage(tgt.to_s, 'Usage: qup [Quote ID]');
        else
          q = @qdb[params[0].to_i]
          return if q.nil?

          if q.voters.include? snd.getNick
            src.sendNotice(snd.getNick, 'You have already voted on this quote')
            return
          end
          q.voters << snd.getNick
          q.vplus += 1
          save
          src.sendNotice(snd.getNick, 'Your vote has been counted, thank you.')
        end
      elsif cmd == 'qdown'
        if(plen < 1)
          src.sendMessage(tgt.to_s, 'Usage: qdown [Quote ID]');
        else
          q = @qdb[params[0].to_i]
          return if q.nil?

          if q.voters.include? snd.getNick
            src.sendNotice(snd.getNick, 'You have already voted on this quote')
            return
          end
          q.voters << snd.getNick
          q.vminus += 1
          save
          src.sendNotice(snd.getNick, 'Your vote has been counted, thank you.')
        end
      end

      @lastmsg[tgt] = msg
      @lastmsgsnd[tgt] = snd
      @lastmsgtime = Time.now
    end

    def on_mode_change(evt)
    end

    def on_nick_change(evt)
    end

    def on_notice(evt)
    end

    def on_numeric_message(evt)
    end

    def on_part(evt)
    end

    def on_quit(evt)
    end

    def on_topic_change(evt)
    end
  end

  $irc.removeListener($qdblistener) if !$qdblistener.nil?
  $qdblistener = Listener.new
  $irc.addListener($qdblistener)
  reply 'QDB running'
end