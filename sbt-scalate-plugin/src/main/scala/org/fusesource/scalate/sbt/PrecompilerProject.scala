package org.fusesource.scalate.sbt

import _root_.sbt._
import java.io.File
import java.{util => ju}
import scala.collection.jcl
import scala.collection.jcl.Conversions._

/**
 * Precompiles the templates as part of the package action.  For a web project,
 * please instead mix in {{{org.fusesource.scalate.sbt.PrecompilerWebProject}}}.
 */
trait PrecompilerProject extends ScalateProject {
  def precompilerSourcesPath: PathFinder = mainResourcesPath
  def precompilerCompilePath: Path = mainCompilePath
  def precompilerGeneratedSourcesPath: Path = outputPath / "generated-sources" / "scalate"
  def precompilerTemplates: List[String] = Nil
  def precompilerContextClass: Option[String] = None

  lazy val precompileTemplates = precompileTemplatesAction

  def precompileTemplatesAction = precompileTemplatesTask() describedAs("Precompiles the Scalate templates")

  def precompileTemplatesTask() = task {
    withScalateClassLoader { classLoader =>

      // Structural Typing FTW (avoids us doing manual reflection)
      type Precompiler = {
        var sources: Array[File]
        var workingDirectory: File
        var targetDirectory: File
        var templates: Array[String]
        var info: {def apply(v1:String):Unit}
        var contextClass: String
        var bootClassName:String
        def execute(): Unit
      }

      val className = "org.fusesource.scalate.support.Precompiler"
      val precompiler = classLoader.loadClass(className).newInstance.asInstanceOf[Precompiler]

      precompiler.info = (value:String)=>log.info(value)
//      precompiler.sources = precompilerSourcesPath.get.toArray
      precompiler.workingDirectory = precompilerGeneratedSourcesPath.asFile
      precompiler.targetDirectory = precompilerCompilePath.asFile
      precompiler.templates = precompilerTemplates.toArray
      precompiler.contextClass = precompilerContextClass.getOrElse(null)
      precompiler.bootClassName = scalateBootClassName.getOrElse(null)
      precompiler.execute()
      None
    }
  } named ("precompile-templates")

  override def packageAction = super.packageAction dependsOn precompileTemplates
}

/**
 * Supports precompilation of templates in a web project.
 */
trait PrecompilerWebProject extends PrecompilerProject with MavenStyleWebScalaPaths {
  override def precompilerSourcesPath: PathFinder = webappPath +++ super.precompilerSourcesPath
  override def precompilerCompilePath: Path = temporaryWarPath / "WEB-INF" / "classes"
}
