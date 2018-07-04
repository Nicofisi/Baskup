import scala.sys.process._

val uploadToTestServer = taskKey[Unit]("upload the compiled assembly to the test server")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.6",
        organization := "me.nicofisi"
    )),
    name := "Baskup",
    version := "1.2.1",

    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",

    uploadToTestServer := {
      assembly.value
      (s"scp -i D:\\cygwin\\home\\Nicofisi\\.ssh\\id_rsa target/scala-2.12/${name.value}-${version.value}.jar " +
        s"root@nicofi.si:/home/mc/lobby/plugins/Baskup.jar").!
      println("Upload complete")
    },

    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("scala.**" -> "me.nicofisi.baskup.@0").inAll
    ),

    resolvers += "spigot-repo" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",

    libraryDependencies ++= Seq(
      "org.spigotmc" % "spigot-api" % "1.12.2-R0.1-SNAPSHOT" % "provided" intransitive(),
      "com.softwaremill.scalamacrodebug" %% "macros" % "0.4.1", // for debug()
      "com.googlecode.json-simple" % "json-simple" % "1.1.1", // for Metrics.java (bStats)
      "me.nicofisi" %% "common-spigot-stuff" % "2.2"
    )
  )
