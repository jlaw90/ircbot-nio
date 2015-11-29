module Hangman

  if(not defined?($dict))
    $dict = Array.new
    file = File.open('data/hangman/dictionary.txt', 'r');
    file.each { |line| line = line.chomp!;$dict << line if line.length >= 6 }
    file.close
  end

  import 'net.newbiehacker.commodore.event.IrcEventListener'
  import 'net.newbiehacker.bot.UserManager'
  import 'net.newbiehacker.bot.GroupManager'

  require 'net/http'

  class Game
    attr_accessor :word, :guesses, :guessed, :bin

    def initialize(word, guesses)
      @word, @guesses = word, guesses
      @guessed = '';
      @bin = Array.new
      @word.each_byte { |b| @guessed += '_'}
    end

    def guess(str)
      str.downcase!
      count = 0
      str.length.times { |g|
        g = str[g]
        if(g >= ?a && g <= ?z && !guessed.include?(g.chr) && !bin.include?(g.chr))
          t_count = 0
          @word.length.times { |i|
            if @word[i] == g
              t_count += 1
              @guessed[i] = g
            end
          }
          if t_count == 0
            @guesses -= 1
            @bin << g.chr
            if(@guesses == 0)
              return count;
            end
          else
            count += t_count;
          end
        end
      }
      return count
    end
  end

  class Listener
    include IrcEventListener

    attr_accessor :games

    def initialize
      @games = {}
      @definitions = {}
    end

    def define(str)
      return @definitions[str] if @definitions[str] != nil

      # Constants
      search_start = '<div class="pbk"><span class="pg">';
      search_end = '</td> </tr> </table> </div>';

      result_start = '<td>'

      str.downcase!
      http_response = Net::HTTP.get_response(URI.parse(URI.escape('http://dictionary.reference.com/browse/' + str)))
      body = http_response.body
      lidx = 0
      while true do
        sidx = body.index(search_start, lidx)
        break if sidx == nil
        sidx += search_start.length
        lidx = body.index(search_end, sidx)
        break if lidx == nil


        # Get the table that holds the results
        substr = body[sidx..lidx]

        wordType = substr[0..substr.index('<') - 1]
        wordType = wordType[3..-1].chomp(' ')
        wordType = wordType[0..0].upcase + wordType[1..-1].downcase

        # Grab the results
        lidx2 = 0
        while true do
          sidx2 = substr.index(result_start, lidx2)
          break if sidx2 == nil
          sidx2 += result_start.length
          lidx2 = substr.index('<', sidx2)
          break if lidx2 == nil

          result = substr[sidx2..lidx2 - 1].chomp(' ')

          if result.length > 5 then
            d = [wordType, result]
            @definitions[str] = d
            return d
          end
        end
      end
    end

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
    end

    def onKick(evt)
    end

    def onMessage(evt)
      src = evt.getSource
      snd = evt.getSender
      tgt = evt.getTarget
      msg = evt.getMessage
      bu = UserManager.getUser(snd)
      if(bu == nil)
        return
      end
      if msg == 'kill_hangman' && GroupManager.inGroup(bu, 'admin')
        src.sendMessage(tgt.to_s, 'Hangman script killed.');
        src.removeListener(self);
        return;
      end
      if msg == 'start'
        if(@games[snd] != nil)
          src.sendMessage(tgt.to_s, 'You\'re already playing a game of hangman you idiot,  wow...');
          return
        end
        @games[snd] = Game.new($dict[rand($dict.size)], 7)
        game = @games[snd]
        src.sendMessage(tgt.to_s, 'Please type \'gc\' (where c is one or more character in the alphabet) to make a guess!');
        src.sendMessage(tgt.to_s, snd.to_s + ': ' + (game.guessed.upcase.split('') * ' ') + (game.bin.size == 0? '': ', tried: ' + (game.bin * ', ')) + ', guesses left: ' + game.guesses.to_s)
      elsif msg == 'stats'
        wins = bu.getInt('hangman_wins', 0)
        loses = bu.getInt('hangman_loses', 0)
        src.sendMessage(tgt.to_s, snd.to_s + ': wins: ' + wins.to_s + ', losses: ' + loses.to_s + ', win to loss ratio: ' + (wins.to_f / loses.to_f).to_s);
      else
        game = @games[snd]
        if(game != nil)
          if msg[0,1] == 'g'
            guess = msg[1..-1]
            count = game.guess(guess)
            if(game.word == game.guessed)
              src.sendMessage(tgt.to_s, 'Congratulations ' + snd.to_s + '! The word was \'' + game.word + '\'.');
              bu.set('hangman_wins', (bu.getInt('hangman_wins', 0) + 1).to_s);
              @games[snd] = nil;
            elsif game.guesses == 0
              d = define(game.word)
              reply = 'Oh dear ' + snd.to_s + ', you let that poor guy hang.  The word was \'' + game.word + '\''
              if d != nil
                reply += ' (' + 2.chr + d[0] + 2.chr + ': ' + d[1] + ')'
              else
                reply += ' (No definition found)'
              end
              src.sendMessage(tgt.to_s,  reply);
              bu.set('hangman_loses', (bu.getInt('hangman_loses', 0) + 1).to_s);
              @games[snd] = nil;
            else
              src.sendMessage(tgt.to_s, snd.to_s + ': ' + (game.guessed.upcase.split('') * ' ') + (game.bin.size == 0? '': ', tried: ' + (game.bin * ', ')) + ', guesses left: ' + game.guesses.to_s)
            end
          end
        end
      end
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
      if @games[evt.getUser()] != nil
        evt.getSource().sendMessage(evt.getChannel().to_s, 'Game  for ' + evt.getUser().getNick() + ' cancelled.');
        @games[evt.getUser()] = nil;
      end
    end

    def onQuit(evt)
      if @games[evt.getUser()] != nil
        @games[evt.getUser()] = nil;
      end
    end

    def onTopicChange(evt)
    end
  end

  $irc.removeListener($hangmanlistener) if !$hangmanlistener.nil?
  $hangmanlistener = Listener.new
  $irc.addListener($hangmanlistener)
  reply 'Please type start to start a game of hangman (cross-channel)'
  return
end