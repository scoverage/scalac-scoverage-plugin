import org.scalajs.jsenv.phantomjs._
import org.scalajs.jsenv._

import org.scalajs.core.tools.classpath._ 
import org.scalajs.core.tools.io._ 
import org.scalajs.core.tools.logging._

import org.scalajs.core.ir.Utils.{escapeJS, fixFileURI}

import java.io._

class ScoveragePhantomJSEnv (
    phantomjsPath: String = "phantomjs",
    addArgs: Seq[String] = Seq.empty,
    addEnv: Map[String, String] = Map.empty,
    override val autoExit: Boolean = true,
    jettyClassLoader: ClassLoader = null
) extends PhantomJSEnv (phantomjsPath, addArgs, addEnv, autoExit, jettyClassLoader) {
  private[ScoveragePhantomJSEnv] trait WebsocketListener {
    def onRunning(): Unit
    def onOpen(): Unit
    def onClose(): Unit
    def onMessage(msg: String): Unit

    def log(msg: String): Unit
  }
 override def jsRunner(classpath: CompleteClasspath, code: VirtualJSFile,
      logger: Logger, console: JSConsole): JSRunner = {
    new ScoveragePhantomRunner(classpath, code, logger, console)
  }

  override def asyncRunner(classpath: CompleteClasspath, code: VirtualJSFile,
      logger: Logger, console: JSConsole): AsyncJSRunner = {
    new AsyncScoveragePhantomRunner(classpath, code, logger, console)
  }

  override def comRunner(classpath: CompleteClasspath, code: VirtualJSFile,
      logger: Logger, console: JSConsole): ComJSRunner = {
    new ComScoveragePhantomRunner(classpath, code, logger, console)
  }


  protected class ScoveragePhantomRunner(classpath: CompleteClasspath,
      code: VirtualJSFile, logger: Logger, console: JSConsole
  ) extends ExtRunner(classpath, code, logger, console)
       with AbstractScoveragePhantomRunner

  protected class AsyncScoveragePhantomRunner(classpath: CompleteClasspath,
      code: VirtualJSFile, logger: Logger, console: JSConsole
  ) extends AsyncExtRunner(classpath, code, logger, console)
       with AbstractScoveragePhantomRunner

  protected class ComScoveragePhantomRunner(classpath: CompleteClasspath,
      code: VirtualJSFile, logger: Logger, console: JSConsole
  ) extends ComPhantomRunner(classpath, code, logger, console)
       with ComJSRunner with AbstractScoveragePhantomRunner with WebsocketListener 

  protected trait AbstractScoveragePhantomRunner extends AbstractPhantomRunner {
    override protected def createTmpLauncherFile(): File = {
      val webF = createTmpWebpage()

      val launcherTmpF = File.createTempFile("phantomjs-launcher", ".js")
      launcherTmpF.deleteOnExit()

      val out = new FileWriter(launcherTmpF)

      try {
        out.write(
            s"""// Scala.js Phantom.js launcher
               |var page = require('webpage').create();
               |var fs = require('fs');
               |var url = "${escapeJS(fixFileURI(webF.toURI).toASCIIString)}";
               |var autoExit = $autoExit;
               |page.onConsoleMessage = function(msg) {
               |  console.log(msg);
               |};
               |page.onError = function(msg, trace) {
               |  console.error(msg);
               |  if (trace && trace.length) {
               |    console.error('');
               |    trace.forEach(function(t) {
               |      console.error('  ' + t.file + ':' + t.line + (t.function ? ' (in function "' + t.function +'")' : ''));
               |    });
               |  }
               |
               |  phantom.exit(2);
               |};
               |page.onCallback = function(data) {
               |  if (!data.action) {
               |    console.error('Called callback without action');
               |    phantom.exit(3);
               |  } else if (data.action === 'exit') {
               |    phantom.exit(data.returnValue || 0);
               |  } else if (data.action === 'setAutoExit') {
               |    if (typeof(data.autoExit) === 'boolean')
               |      autoExit = data.autoExit;
               |    else
               |      autoExit = true;
               |  } else if (data.action == 'require.fs') {
               |    if(data.method == 'separator') {
               |      return JSON.stringify(fs.separator);
               |    } else {
               |      var ret = fs[data.method].apply(this, data.args);
               |      return JSON.stringify(ret);
               |    }
               |  } else {
               |    console.error('Unknown callback action ' + data.action);
               |    phantom.exit(4);
               |  }
               |};
               |page.open(url, function (status) {
               |  if (autoExit || status !== 'success')
               |    phantom.exit(status !== 'success');
               |});
               |""".stripMargin)
      } finally {
        out.close()
      }

      logger.debug(
          "PhantomJS using launcher at: " + launcherTmpF.getAbsolutePath())

      launcherTmpF
    }
  }
}

object ScoveragePhantomJSEnv extends PhantomJSEnv {
}


