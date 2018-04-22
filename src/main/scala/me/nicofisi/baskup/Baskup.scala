package me.nicofisi.baskup

import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Logger

import scala.collection.JavaConverters._
import com.softwaremill.debug.DebugMacros._
import org.bukkit.Bukkit

/* TODO config for:
 * - where to backup to
 * - backup file name pattern
 * - max amount of backups for each script
 * - delete backups older than X
 * - list of ignored scripts
 */

object Baskup {
  val plugin: BaskupJava = BaskupJava.get()
  val logger: Logger = plugin.getLogger
  var watcher: WatchService = _
  private var shuttingDown = false

  def onEnable(): Unit = {
    val skript = Bukkit.getPluginManager.getPlugin("Skript")
    if (skript == null) {
      logger.severe("Skript not found. Baskup won't do anything.")
      plugin.setEnabledPublic(false)
      return
    }

    val scriptsBackupDir = new File(skript.getDataFolder, "backups/scripts")

    val scriptsDir = new File(skript.getDataFolder, "scripts")
    val scriptsPath = scriptsDir.toPath

    watcher = scriptsPath.getFileSystem.newWatchService()

    // start listening to changes in files in plugins/Skript/scripts and all subdirectories
    Files.walkFileTree(scriptsPath, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)
        FileVisitResult.CONTINUE
      }
    })

    val t = new Thread(() => {
      try {
        while (true) {
          val watchKey = watcher.take()

          val events = watchKey.pollEvents()
          events.asScala.foreach { case event: WatchEvent[Path] =>
            val path = event.context()

            if (!List("-", ".").exists(path.toString.startsWith)) {
              debug(event.kind.name, path.toAbsolutePath, path.getParent, path.getRoot)
              event.kind().`type`()
              logger.info(event.kind.name + ": " + path.toAbsolutePath.toString)
            }
          }
          watchKey.reset()
        }
      } catch {
        case ex: ClosedWatchServiceException =>
          if (!shuttingDown) {
            logger.severe("The file update watcher has been unexpectedly closed.")
            logger.severe("No further scripts will be backed up before a restart.")
            ex.printStackTrace()
          }
      }
    })
    t.setName("Baskup File Watcher Thread")
    t.start()
  }

  def runBackup(scriptFile: File): Unit = {

  }

  def onDisable(): Unit = {
    shuttingDown = true
    Option(watcher).foreach(_.close())
  }
}
