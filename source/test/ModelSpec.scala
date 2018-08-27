import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.Play
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test._

import scala.None

@RunWith(classOf[JUnitRunner])
class ModelSpec extends Specification {

  // -- Date helpers
  
  def dateIs(date: java.util.Date, str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date) == str
  
  // --
  "Regex" should {

    "be found" in {
      running(FakeApplication()) {

        val input = "You should not have said it."
        val regex = "You (should(n't| not) )have said( it)?".r
        val s = regex.findFirstIn(input)
        println(s)
        s must not equalTo (None)
      }
    }
  }
}