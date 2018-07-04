package me.nicofisi.baskup

import me.nicofisi.commonspigotstuff._
import Baskup.pluginInfo
import me.nicofisi.commonspigotstuff.commands.{CCommandExecutor, CCommandNoArgs, CParentCommand}
import me.nicofisi.commonspigotstuff.requirements.ReqHasPermission

object BaskupCommandExecutor extends CCommandExecutor(List(
  CParentCommand(List("baskup", "bsk"),
    CCommandNoArgs(List("reload"), List(ReqHasPermission("baskup.reload")), sender => {
      sender.sendColored("Reloading the config..")
      Config.reloadConfig()
      sender.sendColored("The config has been successfully reloaded")
    })
  )
))
