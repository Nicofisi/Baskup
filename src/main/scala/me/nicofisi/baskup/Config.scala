package me.nicofisi.baskup

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import org.bukkit.configuration.file.YamlConfiguration

import scala.collection.JavaConverters._
import scala.concurrent.duration._

object Config {
  var configFile: File = _
  var yaml: YamlConfiguration = _

  /** The current config version */
  val CurrentConfigVersion = 2

  // The config values
  var confBackupLocation: File = _
  var confBackupWhitelist: List[File] = _
  var confBackupBlacklist: List[File] = _
  var confDeleteBackupsOlderThanDays: Int = _

  private def loadDefaultValues(): Unit = {
    confBackupLocation = new File(skriptDataFolder, "backups/scripts")
    confBackupWhitelist = Nil
    confBackupBlacklist = Nil
    confDeleteBackupsOlderThanDays = 0
  }

  private def configDefaults: Map[String, Any] = Map(
    "directory-with-backups" -> "plugins/Skript/backups/scripts",
    "backup-whitelist" -> new java.util.ArrayList(),
    "backup-blacklist" -> new java.util.ArrayList(),
    "delete-backups-older-than-days" -> 0
  )

  private def parseConfig(): Unit = yaml.getValues(false).forEach {
    case ("directory-with-backups", value: String) =>
      confBackupLocation = new File(value)

    case ("backup-whitelist", value: java.util.List[_]) =>
      confBackupWhitelist = value.asScala.toList.map(name => new File(skriptDataFolder, "scripts/" + name.asInstanceOf[String]))

    case ("backup-blacklist", value: java.util.List[_]) =>
      confBackupBlacklist = value.asScala.toList.map(name => new File(skriptDataFolder, "scripts/" + name.asInstanceOf[String]))

    case ("delete-backups-older-than-days", value: Integer) =>
      confDeleteBackupsOlderThanDays = value

    case ("config-version", _) => // do nothing

    case (key, value) => logWarning(s"Your config contains an unknown option, key: $key, value: $value")
  }

  private def handleVersionIncrease(oldConfig: YamlConfiguration, newConfig: YamlConfiguration, fromVersion: Int): Unit = {
    fromVersion match {
      case _ => // do nothing
    }
  }

  private val configHeader: String =
    s"""
       | `directory-with-backups` can be either the full path or relative to the root directory of the server
       |
       | `backup-whitelist` should be `[]` if you want to use `backup-blacklist` instead or don't use any filters at all.
       | if you do want to use the backup whitelist, this part of the config should like this:
       | ```
       | backup-whitelist:
       |   - back-me-up.sk
       |   - pls-do-it.sk
       | ```
       |
       | `backup-blacklist` should be `[]` if you want to use `backup-whitelist` instead or don't use any filters at all.
       | if you do want to use the backup blacklist, this part of the config should like this:
       | ```
       | backup-blacklist:
       |   - dont-back-me-up.sk
       |   - pls-dont-do-it.sk
       | ```
       |
       | `delete-backups-older-than-days` should be set to `0` if you want to disable the feature, or else
       | to some number, like `30`.
       |
       | `config-version` should stay set to `$CurrentConfigVersion`. Please don't touch this
       |
       | If you need help, join https://discord.gg/0l3WlzBPKX7WNjkf and talk to Nicofisi#4467
    """.stripMargin

  private def afterReload(): Unit = {
    Baskup.oldBackupsDeleteTask.foreach(_.cancel())
    if (confDeleteBackupsOlderThanDays > 0) {
      Baskup.oldBackupsDeleteTask = Some(runTaskTimerAsync(2.minutes, 1.hour + 15.minutes, () => {
        Files.walkFileTree(Config.confBackupLocation.toPath, new SimpleFileVisitor[Path]() {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            if (file.toString.toLowerCase.endsWith(".sk")
              && System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis > Config.confDeleteBackupsOlderThanDays.days.toMillis) {
              Files.delete(file)
            }
            FileVisitResult.CONTINUE
          }
        })
      }))
    }
  }

  def reloadConfig(): Unit = {
    configFile = new File(Baskup.javaPlugin.getDataFolder, "config.yml")

    loadDefaultValues()

    if (!configFile.exists()) {
      yaml = new YamlConfiguration()
      setDefaultsInConfig(yaml)
      yaml.set("config-version", CurrentConfigVersion)
      yaml.options().header(configHeader)
      yaml.save(configFile)
    } else {
      require(configFile.isFile, "config.yml must be a regular file")
      yaml = YamlConfiguration.loadConfiguration(configFile)

      val savedConfigVersion = yaml.getInt("config-version")
      if (savedConfigVersion != CurrentConfigVersion) {
        val oldConfigsDir = new File(Baskup.javaPlugin.getDataFolder, "old-configs")
        if (!oldConfigsDir.exists()) {
          oldConfigsDir.mkdirs()
        }
        if (savedConfigVersion > CurrentConfigVersion) {
          val pluginName = Baskup.javaPlugin.getName
          logWarning(s"It appears that the highest config file version understandable by this version of $pluginName")
          logWarning("is lower than the one in your current config file. Did you perhaps downgrade the plugin")
          logWarning("or manually change the config version in the config file? Please never do that again.")
          logWarning("To fix the issue you need to delete/move the config.yml, restart the server for the config")
          logWarning(s"to be recreated, and manually fill it in again. Sorry, but that's not a fault of $pluginName!")
          throw new IllegalStateException()
        } else {
          logInfo(s"The config is going to be updated from v$savedConfigVersion to v$CurrentConfigVersion now")
          var updatedConfig = yaml
          (savedConfigVersion until CurrentConfigVersion).foreach { fromVersion =>
            logInfo(s"Updating the config from v$savedConfigVersion# to v$CurrentConfigVersion#...")
            val configToUpdate = new YamlConfiguration()
            handleVersionIncrease(updatedConfig, configToUpdate, fromVersion)
            updatedConfig = configToUpdate
          }
          yaml = updatedConfig
          setDefaultsInConfig(yaml)
          yaml.options().header(configHeader)
          yaml.set("config-version", CurrentConfigVersion)
          Files.move(configFile.toPath, new File(oldConfigsDir, s"config-${System.currentTimeMillis()}.yml").toPath)
          yaml.save(configFile)
          logInfo("The config has been successfully updated")
        }
      }
      parseConfig()
    }

    afterReload()
  }

  def setDefaultsInConfig(yaml: YamlConfiguration): Unit = {
    configDefaults.filterNot { case (key, _) => yaml.contains(key) }.foreach { case (key, value) =>
      yaml.set(key, value)
    }
  }
}
