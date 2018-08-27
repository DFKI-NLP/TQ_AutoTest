package models

import java.util.Date

import anorm.SqlParser._
import anorm._
import play.api.db.DB

import play.api.Play.current
import play.api.libs.json.{Json, JsArray}

import scala.collection.mutable.ListBuffer

/**
  * Created by Renlong.Ai@dfki.de on 29.12.16.
  */
object Dashboard {

  def getEngineCount: Int = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT count(DISTINCT client) as c FROM reports
        """
      ).as(SqlParser.int("c").single)

    }
  }

  def getReportCount: Int = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT count(*) as c FROM reports
        """
      ).as(SqlParser.int("c").single)

    }
  }

  case class lastReport(id:Int,client:String,time:String)
  val simpleLast = {
    get[String]("client") ~
    get[Date]("time") ~
      get[Int]("id") map {
      case client ~ time ~ id => lastReport(id,client,time.toString.replace(".0", ""))
    }
  }


  def getLastReports():JsArray = {
    DB.withConnection { implicit connection =>

      val ids = SQL(
        """select * from reports where id in (select max(id) as id from reports group by client)
        """
      ).as(simpleLast *)

      var result = new JsArray()
      var singles = new ListBuffer[Int]()
      for(last <- ids){
        singles.clear()
        //not using group by since some reports have no warning
        var arr = SQL(
          s"""select count(*) as c from sentences where reportid=${last.id} and pass=1
          """
        ).as(SqlParser.int("c").single)

        singles += arr

        arr = SQL(
          s"""select count(*) as c from sentences where reportid=${last.id} and pass=2
          """
        ).as(SqlParser.int("c").single)

        singles += arr

        arr = SQL(
          s"""select count(*) as c from sentences where reportid=${last.id} and pass=3
          """
        ).as(SqlParser.int("c").single)

        singles += arr

        result = result.append(Json.obj("time"->last.time, "client"->last.client, "value"->singles.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))))
      }

      result
    }
  }

}
