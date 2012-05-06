import sbt._
import Keys._
import com.github.retronym.SbtOneJar.oneJar

object ClissonServerBuild extends Build {
  val filesToInclude = SettingKey[Seq[String]]("files-to-include", "Files to include in the distributable zip, in addition to one-jar jar")
  val zip = TaskKey[File]("zip", "Builds the distributable zip file")
  
  val zipSettings = Seq(
    zip <<= (target, name, version, oneJar, filesToInclude in zip, streams) map { 
      (buildTarget, projectName, projectVersion, oneJarFile, extraFiles, s) =>
      val dirName = projectName + "-" + projectVersion
      val output = new java.io.File(buildTarget, dirName + ".zip")
      val entries = (extraFiles.map(file) :+ oneJarFile) map (path => (path, dirName + "/" + path.getName))
      s.log.info("Packaging " + output + " ...")
      IO.zip(entries, output)
      s.log.info("Done packaging.")
      output
    }
  )
  
  lazy val root = Project(id       = "clisson-server",
                          base     = file("."),
                          settings = Project.defaultSettings ++ zipSettings)
}
