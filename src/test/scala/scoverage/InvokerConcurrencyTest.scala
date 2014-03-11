package scoverage

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.OneInstancePerTest
import java.io.File
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.collection.breakOut

/**
 * Verify that [[Invoker.invoked()]] is thread-safe
 */
class InvokerConcurrencyTest extends FunSuite with BeforeAndAfter {

  val measurementFile = new File("invoker-test.measurement")

  before {
    measurementFile.delete()
    measurementFile.createNewFile()
  }

  test("calling Invoker.invoked on multiple threads does not corrupt the measurement file") {

    val testIds: Set[Int] = (1 to 1000).toSet

    // Create 1k "invoked" calls on the common thread pool, to stress test
    // the method
    val futures: List[Future[Unit]] = testIds.map { i: Int =>
      future {
        Invoker.invoked(i, measurementFile.toString)
      }
    }(breakOut)

    futures.foreach(Await.result(_, 1.second))

    // Now verify that the measurement file is not corrupted by loading it
    val idsFromFile = IOUtils.invoked(measurementFile).toSet

    idsFromFile === testIds
  }

  after {
    measurementFile.delete()
  }
}
