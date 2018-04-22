lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.5"
    )),
    name := "Baskup",
    resolvers += "spigot-repo" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
    libraryDependencies += "org.spigotmc" % "spigot-api" % "1.12.2-R0.1-SNAPSHOT" % "provided" intransitive(),
    libraryDependencies += "com.softwaremill.scalamacrodebug" %% "macros" % "0.4.1",
    version := "1.0"
  )
