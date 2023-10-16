package commands;

public class BotCommonCommands {
    @AppBotCommands(name = "/hello", description = "when receives request, replies with greeting",showInHelp = true)
    String hello() {
        return "how is it going?";
    }
    @AppBotCommands(name = "/bye", description = "when request received, says goodbye ", showInHelp = true)
    String bye() {
        return "are you leaving? fine.";
    }
    @AppBotCommands(name = "/help", description = "when request received, shows available commands", showInKeyboard = true)
    String help() {
        return "You have used command /help. Later, here could be added some new commands.";
    }

}
