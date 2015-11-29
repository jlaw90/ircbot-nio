package net.newbiehacker.bot;

/**
 * Created by IntelliJ IDEA.
 * User: James
 * Date: 21-Jun-2010
 * Time: 00:30:22
 * To change this template use File | Settings | File Templates.
 */
public final class Command {
    private String name;
    private String help;
    private String lang;
    private String script;
    private String[] params;
    private String[] groups;

    public Command(String name, String[] groups, String[] params, String help, String lang, String script) {
        this.name = name;
        this.groups = groups;
        this.params = params;
        this.help = help;
        this.lang = lang;
        this.script = script;
    }

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    public String getLanguage() {
        return lang;
    }

    public String[] getParameters() {
        return params;
    }

    public String getHelp() {
        return help;
    }

    public boolean canRun(BotUser b) {
        for(String s: groups)
            if(GroupManager.inGroup(b, s))
                return true;
        return false;
    }
}
