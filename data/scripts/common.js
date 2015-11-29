importPackage(java.lang);
function reply(message) {
  if(rt == 0)
    irc.sendMessage(dest, message);
  else if(rt == 1)
    irc.sendNotice(dest, message);
}
