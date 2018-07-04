package me.nicofisi.baskup

import java.io.{File, PrintWriter}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Date

import me.nicofisi.commonspigotstuff._
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitTask

import scala.io.Source

object Baskup extends Listener {
  var watcher: WatchService = _
  var scriptsDir: File = _
  var scriptsPath: Path = _
  val fileNameDateFormat = new SimpleDateFormat("YYYY-MM-DD_HH-mm-ss")
  var oldBackupsDeleteTask: Option[BukkitTask] = None
  private var shuttingDown = false

  implicit val pluginInfo: PluginInfo = PluginInfo(
    plugin = BaskupJava.instance,
    primaryColor = 'e',
    secondaryColor = '7'
  )

  /**
    * A file which at the moment contains only the path of the latest backup file
    */
  val DataFileName = ".baskup_data"

  def onEnable(): Unit = {
    if (!pluginExists("Skript")) {
      logError("Skript not found. Baskup won't do anything.")
      BaskupJava.instance.setEnabledPublic(false)
      return
    }

    Config.reloadConfig()

    BaskupJava.instance.getCommand("baskup").setExecutor(BaskupCommandExecutor)

    scriptsDir = new File(skriptDataFolder, "scripts")
    scriptsPath = scriptsDir.toPath

    Files.walkFileTree(scriptsPath, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (file.toString.toLowerCase.endsWith(".sk")
          && (Config.confBackupWhitelist.isEmpty || Config.confBackupWhitelist.contains(file.toFile))
          && (Config.confBackupBlacklist.isEmpty || !Config.confBackupBlacklist.contains(file.toFile))) {
          runBackup(file.toFile, ignoreIfSame = true)
        }
        FileVisitResult.CONTINUE
      }
    })

    val t = new Thread(() => {
      try {
        new WatchDir(scriptsPath, true, {
          case (kind, path) =>
            if (path.toString.toLowerCase.endsWith(".sk")
              && kind == StandardWatchEventKinds.ENTRY_MODIFY
              && Files.isRegularFile(path)) {
              try {
                runBackup(path.toFile)
              } catch {
                case ex: Exception =>
                  logWarning("An error has occurred while trying to backup " + path.toString)
                  ex.printStackTrace()
              }
            }
        }).processEvents()
      } catch {
        case ex: ClosedWatchServiceException =>
          if (!shuttingDown) {
            logError("The file update watcher has been unexpectedly closed.")
            logError("No further scripts will be backed up before a restart.")
            ex.printStackTrace()
          }
      }
    })
    t.setName("Baskup File Watcher Thread")
    t.start()
  }

  /** Returns Skript#getDataFolder */
  def skriptDataFolder: File = Bukkit.getPluginManager.getPlugin("Skript").getDataFolder

  /**
    * Backs the file up. Fails if the path of the script contains '.sk' somewhere before the end
    *
    * @param scriptFile An existing regular file which is a, possibly recursive, children of `scriptsPath`
    */
  def runBackup(scriptFile: File, ignoreIfSame: Boolean = false): Unit = {
    getExistingBackupDir(scriptFile) match {
      case Some(scriptBackupDir) =>
        // script-without-sk-2018-02-28_01-06-55.sk
        val dataFile = new File(scriptBackupDir, DataFileName)
        try {
          if (ignoreIfSame && dataFile.exists()) {
            val lastBackupFile = new File(scriptBackupDir, Source.fromFile(dataFile).mkString.trim)
            if (lastBackupFile.exists()) {
              if (Source.fromFile(lastBackupFile).getLines().sameElements(Source.fromFile(scriptFile).getLines())) {
                return
              }
            }
          }
        } catch {
          case ex: Exception =>
            logError("An error has occurred while checking whether "
              + scriptFile.getAbsolutePath + " needs to be backed up (it will be backed up regardless)")
            ex.printStackTrace()
        }
        val fileName = scriptFile.getName.dropRight(3) + "-" + fileNameDateFormat.format(new Date()) + ".sk"
        val backupFile = new File(scriptBackupDir, fileName)
        if (backupFile.exists()) {
          logWarning(s"${backupFile.getAbsolutePath} somehow already exists, " +
            s"so ${scriptFile.getAbsolutePath} won't be backed up this time")
        } else {
          Files.copy(scriptFile.toPath, backupFile.toPath)
          val pw = new PrintWriter(dataFile)
          pw.write(fileName)
          pw.close()
        }
      case None => warnAboutDotSkBeforeEnd(scriptFile)
    }
  }

  def warnAboutDotSkBeforeEnd(scriptFile: File): Unit =
    logWarning(s"The path of ${scriptFile.getAbsolutePath} contains '.sk' somewhere before the end. "
      + "The script won't be backed up until you fix this by renaming either the script "
      + "or one of the parent directories, whichever needed")

  def getExistingBackupDir(scriptFile: File): Option[File] = {
    val relScriptPath = scriptsPath.relativize(scriptFile.toPath)
    if (scriptFile.getAbsolutePath.toLowerCase.indexOf(".sk") != scriptFile.getAbsolutePath.length - 3) {
      None
    } else {
      val backupDir = new File(Config.confBackupLocation, relScriptPath.toString)
      backupDir.mkdirs()
      Some(backupDir)
    }
  }

  def onDisable(): Unit = {
    shuttingDown = true
    Option(watcher).foreach(_.close())
  }
}
