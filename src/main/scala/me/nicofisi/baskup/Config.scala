package me.nicofisi.baskup

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import me.nicofisi.commonspigotstuff._
import Baskup.pluginInfo
import me.nicofisi.commonspigotstuff.config.CConfig

object Config extends CConfig(currentConfigVersion = 2, pluginInfo){
  var confBackupLocation: File = _
  var confBackupWhitelist: List[File] = _
  var confBackupBlacklist: List[File] = _
  var confDeleteBackupsOlderThanDays: Int = _

  override def loadDefaultValues(): Unit = {
    confBackupLocation = new File(Baskup.skriptDataFolder, "backups/scripts")
    confBackupWhitelist = Nil
    confBackupBlacklist = Nil
    confDeleteBackupsOlderThanDays = 0
  }

  override def configDefaults: Map[String, Any] = Map(
    "directory-with-backups" -> "plugins/Skript/backups/scripts",
    "backup-whitelist" -> new java.util.ArrayList(),
    "backup-blacklist" -> new java.util.ArrayList(),
    "delete-backups-older-than-days" -> 0
  )


  override def parseConfig(map: Map[String, AnyRef]): Unit = map.foreach {
    case ("directory-with-backups", value: String) =>
      confBackupLocation = new File(value)

    case ("backup-whitelist", value: java.util.List[_]) =>
      confBackupWhitelist = value.asScala.toList.map(name => new File(Baskup.skriptDataFolder, "scripts/" + name.asInstanceOf[String]))

    case ("backup-blacklist", value: java.util.List[_]) =>
      confBackupBlacklist = value.asScala.toList.map(name => new File(Baskup.skriptDataFolder, "scripts/" + name.asInstanceOf[String]))

    case ("delete-backups-older-than-days", value: Integer) =>
      confDeleteBackupsOlderThanDays = value

    case ("config-version", _) => // do nothing

    case (key, value) => logWarning(s"Your config contains an unknown option, key: $key, value: $value")(pluginInfo)
  }

  override def afterReload(): Unit = {
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
      })(pluginInfo))
    }
  }
}
