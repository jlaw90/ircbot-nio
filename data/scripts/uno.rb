module Uno
  # import
  import 'net.newbiehacker.commodore.event.IrcEventListener'
  import 'net.newbiehacker.commodore.MessageTransceiverType'
  import 'net.newbiehacker.bot.UserManager'
  import 'net.newbiehacker.bot.GroupManager'

  # IRC color constants
  BOLD = 2.chr
  COLOR = 3.chr
  COLOR_ORIG = 15.chr
  COLOR_BLU = COLOR + '12'
  COLOR_YEL = COLOR + '08'
  COLOR_RED = COLOR + '04'
  COLOR_GRE = COLOR + '03'
  UNOSTR = COLOR_YEL + 'U' + COLOR_RED + 'N' + COLOR_BLU + 'O' + COLOR_ORIG

  class Card
    attr_accessor :color, :rank

    # Color constants
    BLACK = -1
    BLUE = 0
    YELLOW = 1
    RED = 2
    GREEN = 3


    # Rank constants for action cards
    SKIP = 10
    DRAW_TWO = 11
    REVERSE = 12
    WILD = 13
    WILD_DRAW_FOUR = 14

    def initialize(color, rank)
      @color, @rank = color, rank
    end

    def matches?(card)
      return false if card == nil
      return true if card.rank == WILD_DRAW_FOUR or card.rank == WILD
      return card.rank == @rank || card.color == @color
    end

    def get_score
      return @rank if @rank >= 0 && rank < 10
      return 50 if @rank == WILD or @rank == WILD_DRAW_FOUR
      return 20
    end

    def ==(other)
      return false if other == nil
      return true if @rank == WILD and other.rank == WILD
      return true if @rank == WILD_DRAW_FOUR and other.rank == WILD_DRAW_FOUR
      return other.rank == @rank && other.color == @color
    end

    def to_s
      # Wild and Wd4 don't follow convention, so return them first
      if @rank == WILD
        return COLOR_YEL + 'w' + COLOR_BLU + 'i' + COLOR_RED + 'l' + COLOR_GRE + 'd' + COLOR_ORIG +
                (@color == BLACK ? '' : ' [' + Card::get_color(@color) + COLOR_ORIG + ']').upcase
      elsif @rank == WILD_DRAW_FOUR
        return COLOR_YEL + 'w' + COLOR_BLU + 'd' + COLOR_RED + '4' + COLOR_ORIG +
                (@color == BLACK ? '' : ' [' + Card::get_color(@color) + COLOR_ORIG + ']').upcase
      end

      # Append color
      str = Card::get_color(@color)

      # Append rank
      str += @rank.to_s if @rank < 10
      str += 's' if @rank == SKIP
      str += 'd2' if @rank == DRAW_TWO
      str += 'r' if @rank == REVERSE

      # Append color normaliser and leave at top of the stack
      str += COLOR_ORIG
      str.upcase
    end

    def Card::from_s(str)
      str.downcase!

      strs = str.split
      pcol = strs.length == 1 ? nil : strs[1][0]
      str = strs[0]
      wcol = BLACK
      if pcol != nil
        wcol = BLUE if pcol == ?b
        wcol = YELLOW if pcol == ?y
        wcol = RED if pcol == ?r
        wcol = GREEN if pcol == ?g
      end

      # wd4 matching is complicated, make a list of strings!
      wd4match = []
      foura = ['4', 'four']
      drawa = ['draw', 'd']
      wilda = ['wild', 'w']
      foura.each {|f| drawa.each {|d| wilda.each {|w| wd4match << w + d + f}}}

      # Check for wild or wd4 first
      if str == 'w' || str == 'wild'
        return Card.new(wcol, WILD)
      elsif wd4match.include? str
        return Card.new(wcol, WILD_DRAW_FOUR)
      end

      # Parse color
      color = -1
      clr = str[0]
      color = BLUE if clr == ?b
      color = YELLOW if clr == ?y
      color = RED if clr == ?r
      color = GREEN if clr == ?g

      return nil if color == -1

      # Parse suit
      str = str[1..-1]

      # Check for action cards
      return Card.new(color, SKIP) if str == 's' || str == 'skip'
      return Card.new(color, REVERSE) if str == 'r' || str == 'reverse' || str == 'rev'
      return Card.new(color, DRAW_TWO) if str == 'd2' || str == 'drawtwo' || str == 'draw2' || str == 'dtwo'

      # Check it's a valid number...
      begin
        rank = Integer(str)
      rescue
        return nil
      end
      return nil if rank < 0 || rank > 9
      return Card.new(color, rank)
    end

    def Card::get_color(color)
      return COLOR_BLU + 'B' if color == BLUE
      return COLOR_YEL + 'Y' if color == YELLOW
      return COLOR_RED + 'R' if color == RED
      return COLOR_GRE + 'G' if color == GREEN
      return COLOR_RED + '[ERROR]' + COLOR_ORIG
    end
  end

  class Deck
    attr_accessor :cards

    def initialize
      @cards = []

      # Initialise colored cards (2 of each except for '0' cards)
      (0..3).each do |i|
        # 0 card
        @cards << Card.new(i, 0)

        # Colored and action cards
        (1..12).each do |j|
          @cards << Card.new(i, j)
          @cards << Card.new(i, j)
        end
      end

      # Wild and WD4 cards
      (1..4).each do |i|
        @cards << Card.new(Card::BLACK, Card::WILD);
        @cards << Card.new(Card::BLACK, Card::WILD_DRAW_FOUR);
      end

      # Shuffle
      shuffle!
    end

    def pop
      @cards.pop
    end

    def shuffle!
      @cards = @cards.sort_by { rand }
    end

    def rebuild(cards)
      @cards = cards
      @cards.each {|c| c.color == Card::BLACK if c.rank == Card::WILD or c.rank == Card::WILD_DRAW_FOUR}
      shuffle!
    end

    def size
      cards.size
    end
  end

  class Player
    attr_accessor :user, :cards, :score, :precalleduno, :calleduno, :idlecount, :idletime

    def initialize(user)
      @user = user
      @cards = []
      @score = 0
      @idlecount = 0
      @idletime = 0
      @calleduno = false
      @precalleduno = false
    end
  end

  class Game
    # Direction
    CLOCKWISE = 1
    COUNTERCLOCKWISE = -1

    # Game state
    RECRUIT = 1
    PLAYING = 2
    COLOR = 3
    OVER = -1

    attr_accessor :deck, :discard, :players, :direction, :curplayer, :gamestate, :actiontime, :chan, :con,
                  :bot, :score_limit

    def initialize(chan, con, bot)
      @deck = Deck.new
      @discard = []
      @direction = CLOCKWISE
      @curplayer = 0
      @gamestate = OVER
      @chan = chan
      @con = con
      @bot = bot
      @players = []
      @score_limit = 100
    end

    def start
      @deck = Deck.new
      @discard = []
      @direction = CLOCKWISE
      @curplayer = 0
      #Shuffle the players around
      @players = @players.sort_by {rand}
      # Choose a random player
      @curplayer = rand(@players.size)
      @players.each {|p| p.cards = [];p.calleduno = false;p.precalleduno = false;}
      deal
      @players.each_index {|n| tellhand(@players[n]) if n != @curplayer}
      @discard << @deck.pop
      @curplayer = (@curplayer - @direction) % @players.size
      @gamestate = PLAYING
      play(@discard.last)
      announce
    end

    def start?
      return @players.size >= 2
    end

    def deal
      @players.each { |p| 7.times {p.cards << draw}}
    end

    def can_play?(card)
      return @discard.last.matches(card)
    end

    def draw
      if @deck.size == 0
        top = @discard.pop
        @deck.rebuild(@discard)
        @discard = [top]
      end
      @deck.pop
    end

    def drawx(player, count)
      player.calleduno = false
      first = true
      str = 'You drew: '
      count.times do
        card = draw
        str += (first ? '' : ', ') + card.to_s
        player.cards << card
        first = false
      end
      @con.sendNotice(player.user.getNick, str)
    end

    def tellhand(p)
      @con.sendNotice(p.user.getNick(), 'Your hand: ' + (p.cards * ', '));
    end

    def announce
      turnstr = ['it''s your turn', 'wake up', 'play your hand', 'let''s see what you can do', 'show \'em', 'let\'s go']
      @con.sendMessage(@chan, BOLD + @players[@curplayer].user.getNick + BOLD + ', ' + turnstr[rand * turnstr.size] \
        + '. Discard pile: [' + @discard.last.to_s + ']');
      tellhand(@players[@curplayer])
    end

    def play(card)
      @discard << card

      # Played a wild or wild draw four without choosing a color
      if card.color == Card::BLACK
        @con.sendMessage(@chan, BOLD + @players[@curplayer].user.getNick + BOLD + ', please select a color by typing \'@color \' +  r/g/b/y')
        @gamestate = COLOR
        return
      end

      # Played a Reverse
      if card.rank == Card::REVERSE
        @direction = Game::CLOCKWISE if @direction == Game::COUNTERCLOCKWISE
        @direction = Game::COUNTERCLOCKWISE if @direction == Game::CLOCKWISE
        if(@players.size == 2)
          @curplayer = (@curplayer + @direction) % @players.size
          @con.sendMessage(@chan, 'The turn direction has been reversed (' + BOLD + @players[@curplayer].user.getNick + BOLD + ' was skipped!)')
        else
          @con.sendMessage(@chan, 'The turn direction has been reversed!')
        end
      end

      n = (@curplayer + @direction) % @players.size
      nextp = @players[n]

      # Played a Skip
      if card.rank == Card::SKIP
        @con.sendMessage(@chan, BOLD + nextp.user.getNick + BOLD + ' was skipped!')
        n = (n + @direction) % @players.size
        nextp = @players[n]
      end

      # Played a Draw Two
      if card.rank == Card::DRAW_TWO
        drawx(nextp, 2)
        @con.sendMessage(@chan, BOLD + nextp.user.getNick + BOLD + ' had to draw 2 cards and was skipped!')
        n = (n + @direction) % @players.size
        nextp = @players[n]
      end

      # Played a Wild Draw Four
      if card.rank == Card::WILD_DRAW_FOUR
        drawx(nextp, 4)
        @con.sendMessage(@chan, BOLD + nextp.user.getNick + BOLD + ' had to draw 4 cards and was skipped!')
        n = (n + @direction) % @players.length
        nextp = @players[n]
      end

      @curplayer = n
    end
  end

  class Listener
    include IrcEventListener

    def initialize(con, bot)
      @con = con
      @bot = bot
      @games = {}
      @threads = {}
    end

    def scoreboard(players, won=false)
      # Build a scoreboard (awkward code)
      # Build a dict of [user] = points
      map = {}
      players.each {|p| map[p.user.getNick] = p.score}
      # Sort it
      sorted = map.sort {|a, b| b[1] <=> a[1]}
      # Build the strings for each player...
      arr = []
      winning = ['in the lead', 'flying ahead', 'blazing past', 'going for gold']
      losing = ['struggling', 'falling behind', 'tasting the dust']
      sorted.each_index do |i|
        n = sorted[i][0]
        p = sorted[i][1]
        n = BOLD + n + BOLD
        r = rand >= 0.5
        if i == 0
          if won
            arr << n + ' won with ' + p.to_s + ' points'
          else
            w = winning[rand * winning.size]
            if r
              arr << n + ' is ' + w + ' with ' + p.to_s + ' points'
            else
              arr << w + ' with ' + p.to_s + ' is ' + n
            end
          end
        elsif i == sorted.size - 1
          l = losing[rand * losing.size]
          if r
            arr << n + ' is ' + l + ' with ' + p.to_s + ' points'
          else
            arr << l + ' with ' + p.to_s + ' is ' + n
          end
        else
          if r
            arr << n + ' has ' + p.to_s + ' points'
          else
            arr << ' with ' + p.to_s + ' is ' + n
          end
        end
      end
      arr * ', '
    end

    def end_game(game, player, chan)
      # Calculate scores...
      score = 0
      game.players.each {|p| p.cards.each {|d| score += d.get_score}}
      player.score += score
      @con.sendMessage(chan.to_s, BOLD + player.user.getNick + BOLD + ' earned ' + score.to_s + ' points!');
      if player.score >= game.score_limit or game.players.length == 1
        @con.sendMessage(chan.to_s, BOLD + player.user.getNick + BOLD + ' won this game of ' + UNOSTR + '!');
        @con.sendMessage(chan.to_s, 'The final scores are: ' + scoreboard(game.players, true))
        @games[chan.to_s] = nil
        player.user.set('unoscore', (player.user.getInt('unoscore', 0) + player.score).to_s);
        player.user.set('unowins', (player.user.getInt('unowins', 0) + 1).to_s);
        @threads[chan.to_s].kill
        @threads[chan.to_s] = nil
      end

      # Tell the scores
      @con.sendMessage(chan.to_s, 'Current scores: ' + scoreboard(game.players))
      @con.sendMessage(chan.to_s, 'It\'s not over yet, ' + game.score_limit.to_s + ' is your target, so here we go!');
      game.start
    end

    def delete_player(game, chan, todel)
      game.deck.cards << todel.cards
      game.deck.cards = game.deck.cards.flatten
      game.deck.shuffle!
      game.players.delete(todel)

      if game.players.size == 1
        end_game(game, game.players[0], chan)
        return
      elsif game.curplayer >= game.players.size
        game.curplayer = (game.curplayer + game.direction) % game.players.size
      end
      game.announce
    end

    def on_message(evt)
        u = evt.getSender
        bu = UserManager.getUser(u)
        chan = evt.getTarget
        omsg = evt.getMessage
        msg = evt.getMessage.downcase
        game = @games[chan.to_s]
        curplayer = !game.nil? && game.players.size > 0 && game.players[game.curplayer].user == bu

        # Ignore private messages
        return if chan.getType() != MessageTransceiverType::CHANNEL or bu.nil?

        admin = GroupManager.inGroup(bu, "admin")

        if msg == '@kill'
          if admin
            @con.sendMessage(chan.to_s, 'UNO listener removed from IRC connection');
            @con.removeListener(self);
          end
        elsif (msg == '@start') or (msg == '@s')
          if game.nil?
            game = @games[chan.to_s] = Game.new(chan.to_s, @con, @bot)
            game.gamestate = Game::RECRUIT
            intervals = [0, 15, 10]
            tot = 0
            rem = 0
            intervals.each do |i|
              sleep(i)
              tot += i
              rem = (30 - tot).to_i

              # Generate random messages
              suff = ['to join', 'to get in on the action', 'to play']
              suffix = suff[rand * suff.length]
              @con.sendMessage(chan.to_s, 'A game of ' + UNOSTR + ' is starting in ' + rem.to_s \
                + ' seconds. Type @join ' + suffix + '!')
            end
            sleep(rem)
            if game.start?
              game.start
              $! = nil
              begin
              ithread = @threads[chan.to_s] = Thread.new(@con, game, chan) {
                |src,game,chan|
                lastPlr = game.players[game.curplayer]
                while true do
                  plr = game.players[game.curplayer]
                  if lastPlr != plr
                    lastPlr.idletime = 0
                    lastPlr = plr
                  end
                  sleep(1)
                  plr.idletime += 1;
                  if plr.idletime == 20
                    src.sendNotice(plr.user.nick, 'Hurry up! (10 secs until you are ' + (plr.idlecount == 2? 'kicked': 'skipped') + '!)')
                  elsif plr.idletime == 30
                    plr.idlecount += 1
                    if plr.idlecount == 3
                      src.sendMessage(chan.to_s, plr.user.nick + ' was removed from the game for being idle!')
                      delete_player(game, chan, plr)
                    else
                      game.curplayer = (game.curplayer + game.direction) % game.players.length
                      src.sendMessage(chan.to_s, plr.user.nick + ' was skipped for being idle!')
                      game.announce
                    end
                  end
                end
                }
              rescue
                src.sendMessage(chan.to_s, 'Error (in thread): ' + $!)
              end
              src.sendMessage(chan.to_s, 'Error:' + $! + ' @ ' + $@) if !$!.nil?

            else
              @con.sendMessage(chan.to_s, 'Game cancelled (Not enough players!)');
              @games[chan.to_s] = nil
            end
          end
        elsif (msg == '@join') or msg == '@j'
          if game != nil and game.gamestate == Game::RECRUIT
            exists = false
            game.players.each {|p| exists = true if p.user == bu}
            if !exists
              game.players << Player.new(bu)
              suff = ['has joined this game of', 'is cool, they\'re playing', 'graces us all by playing']
              suffix = suff[rand * suff.size]
              @con.sendMessage(chan.to_s, BOLD + bu.getNick + BOLD + ' ' + suffix + ' ' + UNOSTR + '!');
            end
          end
        elsif game.nil? or game.gamestate == Game::RECRUIT or game.gamestate == Game::OVER
            return
        elsif (game.gamestate == Game::COLOR) and (msg[0, 7] == '@color ') or (msg[0,3] == '@c ')
          if curplayer
            ch = msg[7..-1][0]
            c = game.discard.last
            c.color = Card::RED if ch == ?r
            c.color = Card::YELLOW if ch == ?y
            c.color = Card::GREEN if ch == ?g
            c.color = Card::BLUE if ch == ?b
            if c.color != Card::BLACK
              game.gamestate = Game::PLAYING
              game.play(game.discard.pop)
              game.announce
            else
              @con.sendNotice(bu.getNick, 'Invalid color specified (Please choose r/g/b/y)')
            end
          end
        elsif (msg[0, 3] == '@p ') or (msg[0,6] = '@play ')
          if (game.gamestate == Game::PLAYING) && curplayer
            substr = msg[3..-1]
            c = Card::from_s(substr)
            if c == nil
              @con.sendNotice(bu.getNick, 'Invalid card specified')
              return;
            elsif !game.players[game.curplayer].cards.include?(c)
              @con.sendNotice(bu.getNick, 'You do not have this card')
              return;
            elsif !game.discard.last.matches?(c)
              @con.sendNotice(bu.getNick, 'You can not play this card at this time')
              return;
            end
            player = game.players[game.curplayer]
            deleted = false;
            player.cards.each_index do |i|
              if player.cards[i] == c && !deleted
                player.cards.delete_at i
                deleted = true
              end
            end
            game.play(c)

            if player.cards.size == 1
              if player.precalleduno
                @con.sendMessage(chan.to_s, BOLD + bu.getNick + BOLD + ' called ' + UNOSTR + '!')
                player.calleduno = true
              else
                @con.sendMessage(chan.to_s, BOLD + bu.getNick + BOLD + ' has ' + UNOSTR + ' (but hasn''t called it!)');
              end
            end
            player.precalleduno = false if player.precalleduno
            if player.cards.size == 0
              end_game(game, player, chan)
            end
            if game.gamestate != Game::COLOR
              game.announce
            end
          end
        elsif (msg == '@ch') or (msg == '@c')
          if (game.gamestate == Game::PLAYING)
            # Find out what context the challenge is for

            # Curplayer
            cp = game.players[game.curplayer]

            # guy that was skipped
            p = (game.curplayer - game.direction) % game.players.size
            pp = game.players[p]

            # Wild Draw Four Challenge
            if ((game.discard.last.rank == Card::WILD_DRAW_FOUR) && pp.user == bu)
              # If the previous player could have played another card, we make them draw four cards
              # If not we have to now draw 6!

              # guy who played it
              pr = (p - game.direction) % game.players.size
              prp = game.players[pr]

              established = false
              prp.cards.each {|c| established = true if c.matches?(game.discard[-2])}

              if established
                @con.sendMessage(chan.to_s, 'Challenge was substantiated, ' + BOLD + pr.user.getNick + BOLD +
                        ' had to draw four cards and play was returned to ' + BOLD + cp.user.getNick + BOLD + '!');
                game.drawx(pr, 4)
                game.announce
              else
                @con.sendMessage(chan.to_s, 'Challenge failed, ' + BOLD + pp.user.getNick + BOLD + ' had to draw 2 more cards!')
                game.drawx(pp, 2)
                game.announce
              end
            end

            # UNO Challenge
            if (pp.cards.size == 1) and !pp.calleduno
              # Set calleduno to true so they can't be challenged multiple times
              pp.calleduno = true
              @con.sendMessage(chan.to_s, 'Challenge substantiated, ' + BOLD + pp.user.getNick + BOLD + ' had to draw 2 cards!');
              game.drawx(pp, 2)
            end
          end
        elsif (msg == '@uno') or (msg == '@u')
          if game.gamestate == Game::PLAYING
            if curplayer && game.players[game.curplayer].cards.size == 2
              game.players[game.curplayer].precalleduno = true
            else
              pla = nil
              game.players.each { |p| pla = p if p.user == bu }
              return if pla.nil?
              if pla.cards.size == 1
                pla.calleduno = true
                @con.sendMessage(chan.to_s, BOLD + bu.getNick + BOLD + ' called ' + UNOSTR + '!')
              end
            end
          end
        elsif (msg == '@draw') or (msg == '@d')
          if (game.gamestate == Game::PLAYING) && curplayer
            # Check they can't play anything first...
            pla = game.players[game.curplayer]
            cango = false
            pla.cards.each { |c| cango = true if game.discard.last.matches?(c)}
            if cango
              @con.sendNotice(bu.getNick, 'You do not need to draw a card')
            else
              game.drawx(pla, 1)
              cango = false
              pla.cards.each { |c| cango = true if game.discard.last.matches?(c)}
              str = BOLD + bu.getNick + BOLD + ' drew a card'
              if !cango
                str += ' and skipped their go.'
                @con.sendMessage(chan.to_s, str)
                game.curplayer = (game.curplayer + game.direction) % game.players.size
                game.announce
              else
                str += '.'
                @con.sendMessage(chan.to_s, str)
              end
            end
          end
        elsif admin && (msg[0, 3] == '@e ')
          substr = omsg[3..-1]
          @con.sendMessage(chan.to_s, 'Eval: ' + eval(substr).to_s);
        elsif msg == '@hand'
          game.players.each {|p| game.tellhand(p) if p.user == bu }
        elsif (msg == '@quit') or (msg == '@q')
          todel = nil
          game.players.each {|p|
            if p.user == bu
              todel = p
            end }
          return if todel.nil?

          src.sendMessage(chan.to_s, todel.user.get_nick + ' has quit this game of ' + UNOSTR)
          delete_player(game, chan, todel)
        elsif (msg == '@scores') or (msg == '@s')
          @con.sendNotice(bu.getNick, 'Current scores: ' + scoreboard(game.players))
        elsif msg == '@discard' or msg == '@deck'
          game.announce
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

    def on_mode_change(evt)
    end

    def on_nick_change(evt)
    end

    def on_notice(evt)
    end

    def on_numeric_message(evt)
    end

    def on_part(evt)
      chan = evt.getChannel()
      game = @games[chan.to_s]
      bu = UserManager.getUser(evt.getUser())
      return if bu.nil? or game.nil?
      plr = nil
      game.players.each {|p|
        if p.user == bu
              plr = p
        end }
      return if plr.nil?
      @con.sendMessage(chan.to_s, bu.nick + ' has forfeited this game of ' + UNOSTR + '!')
      delete_player(game, chan, plr)
    end

    def on_quit(evt)
      bu = UserManager.getUser(evt.getUser())
      return if bu.nil?
      @games.each {|game|
        plr = nil
        game.players.each {|p|
        if p.user == bu
              plr = p
        end }
        next if plr.nil?
        chan = game.chan
        @con.sendMessage(chan.to_s, bu.nick + ' has forfeited this game of ' + UNOSTR + '!')
        delete_player(game, chan, plr)
      }
    end

    def on_topic_change(evt)
    end
  end
end

$irc.removeListener $unolistener if !$unolistener.nil?
$unolistener = Uno::Listener.new $irc, $bot
$irc.addListener $unolistener
reply(Uno::UNOSTR + ' script loaded! Please type @start to start a game in your channel!')