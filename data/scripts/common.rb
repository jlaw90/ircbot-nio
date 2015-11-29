require 'java'
def reply(message)
  if $rt == 0
    $irc.sendMessage $dest, message
  elsif $rt == 1
    $irc.sendNotice $dest, message
  end
end
