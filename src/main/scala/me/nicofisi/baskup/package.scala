package me.nicofisi

import java.io.File

import org.bukkit.command.CommandSender
import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.scheduler.BukkitTask

import scala.concurrent.duration.Duration
import scala.runtime.NonLocalReturnControl

package object baskup {
  /** The primary chat color */
  val primaryColor = "&e"
  /** The secondary chat color */
  val secondaryColor = "&7"
  val errorPrefix = "&p[Error]&s"

  /** Returns the location of the jar file containing this class, which should be the plugin's jar file */
  lazy val pluginJarFile = new File(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)

  lazy val dataFolder: File = Baskup.javaPlugin.getDataFolder

  def skriptDataFolder: File = Bukkit.getPluginManager.getPlugin("Skript").getDataFolder


  def logInfo(info: String): Unit = Baskup.javaPlugin.getLogger.info(info)

  def logWarning(warning: String): Unit = Baskup.javaPlugin.getLogger.warning(warning)

  def logError(error: String): Unit = Baskup.javaPlugin.getLogger.severe(error)

  
  def runTask(runnable: Runnable): BukkitTask =
    Bukkit.getScheduler.runTask(Baskup.javaPlugin, enhanceRunnable(runnable))

  def runTaskAsync(runnable: Runnable): BukkitTask =
    Bukkit.getScheduler.runTaskAsynchronously(Baskup.javaPlugin, enhanceRunnable(runnable))

  def runTaskLater(delay: Duration, runnable: Runnable): BukkitTask =
    Bukkit.getScheduler.runTaskLater(Baskup.javaPlugin, enhanceRunnable(runnable), delay.toMillis / 50)

  def runTaskLaterAsync(delay: Duration, runnable: Runnable): BukkitTask =
    Bukkit.getScheduler.runTaskLaterAsynchronously(Baskup.javaPlugin, enhanceRunnable(runnable), delay.toMillis / 50)

  def runTaskTimer(delayToFirst: Duration, delayBetween: Duration, runnable: Runnable): BukkitTask =
    Bukkit.getScheduler.runTaskTimer(Baskup.javaPlugin, enhanceRunnable(runnable), delayToFirst.toMillis / 50, delayBetween.toMillis / 50)

  def runTaskTimerAsync(delayToFirst: Duration, delayBetween: Duration, runnable: Runnable): BukkitTask =
    Bukkit.getScheduler.runTaskTimerAsynchronously(Baskup.javaPlugin, enhanceRunnable(runnable), delayToFirst.toMillis / 50, delayBetween.toMillis / 50)

  def enhanceRunnable(runnable: Runnable): Runnable =
    () => try runnable.run() catch {
      case _: NonLocalReturnControl[_] =>
      // we don't catch other errors
    }


  implicit class StringUtils(str: String) {
    def colored: String =
      ChatColor.translateAlternateColorCodes('&', str.replace("&p", primaryColor).replace("&s", secondaryColor))
  }

  implicit class CommandSenderUtils(sender: CommandSender) {
    def sendColored(message: String): Unit = sender.sendMessage(("&s" + message).colored)
    def sendError(message: String): Unit = sender.sendMessage(message.colored)
  }
}
