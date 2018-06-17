package me.nicofisi.baskup

import org.bukkit.command.{Command, CommandExecutor, CommandSender}

object BaskupCommand extends CommandExecutor {

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    try {
      handleCommand(sender, command, label, args)
    } catch {
      case _: PermissionException =>
      case t: Throwable =>
        sender.sendError(s"An error has occurred while attempting to perform this command: " +
          s"&p${t.getClass.getCanonicalName}${Option(t.getMessage).map(": " + _).getOrElse("")}")
        t.printStackTrace()
    }
    true
  }

  def handleCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Unit = {
    baseCommands.find(_.aliases.contains(label.toLowerCase)) match {
      case Some(CmdMain) =>
        readArg(args, 0, baskupCommandChildren) match {
          case Some(CmdMainReload) =>
            validatePermission(sender, "baskup.reload", "reload Baskup")
            sender.sendColored("Reloading the config...")
            Config.reloadConfig()
            sender.sendColored("&p[Success] &sThe config has been reloaded")
          case Some(_) | None =>
            sender.sendColored(s"Help for &p/$label&s:")
            sender.sendColored(s"&p/$label reload &s- reload the config")
        }
      case None => sender.sendMessage(s"$errorPrefix Unknown command: /$label")
    }
  }

  def readArg(args: Array[String], index: Int, commands: List[BaskupCommand]): Option[BaskupCommand] =
    args.lift(index).flatMap(arg => commands.find(_.aliases.contains(arg)))

  sealed trait BaskupCommand {
    def aliases: List[String]
  }

  sealed case class SeparateCommand(aliases: List[String]) extends BaskupCommand

  object CmdMain extends SeparateCommand(List("baskup", "bsk"))

  val baseCommands = List(CmdMain)

  """
    |    set {_v} to        level of protection of {_e}'s helmet
    |    set {_v} to {_v} + level of protection of {_e}'s chestplate
    |    set {_v} to {_v} + level of protection of {_e}'s leggings
    |    set {_v} to {_v} + level of protection of {_e}'s boots
    |    return {_n} * (1 / (1 - (((6 + {_v}^2)/3) * 0.75) / 100))
  """.stripMargin


  sealed case class MainCommandChildren(aliases: List[String]) extends BaskupCommand

  object CmdMainReload extends MainCommandChildren(List("reload", "r"))

  val baskupCommandChildren = List(CmdMainReload)


  def validatePermission(sender: CommandSender, permission: String, action: String = "perform this action"): Unit = {
    if (!sender.hasPermission(permission)) {
      sender.sendError(s"You need the &p$permission &spermission to &p$action".colored)
      throw new PermissionException
    }
  }

  class PermissionException extends RuntimeException

}
