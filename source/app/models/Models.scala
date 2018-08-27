package models

import java.net.{URLDecoder, URLEncoder}
import java.util
import java.util.Date
import java.util.regex.PatternSyntaxException

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

/**
  * Created by Renlong.Ai@dfki.de on 12.12.16.
  */

//need for presenting categories and barriers in selection list
case class Category(name: String, subs: List[String])

object Category {
  def getCategories(): List[Category] = {

    val lang = LangDirection.getLangDirections()(0).replace("->", "").toLowerCase

    DB.withConnection { implicit connection =>

      val parser: RowParser[(String, String)] = SqlParser.str("category") ~ SqlParser.str("barrier") map (SqlParser.flatten)
      val cates: List[(String, String)] = SQL(
        s"""SELECT DISTINCT category,barrier from rules WHERE length(category)>0 and length(barrier)>0 and direction='$lang' order by barrier
        """
      ).as(parser.*)

      cates.groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) }.map { f => Category(f._1, f._2) }.toList.sortBy(_.name)
    }
  }

  def getCategories(lang: String): List[Category] = {

    DB.withConnection { implicit connection =>

      val parser: RowParser[(String, String)] = SqlParser.str("category") ~ SqlParser.str("barrier") map (SqlParser.flatten)
      val cates: List[(String, String)] = SQL(
        s"""SELECT DISTINCT category,barrier from rules WHERE length(category)>0 and length(barrier)>0 and direction='$lang' order by barrier
        """
      ).as(parser.*)

      cates.groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) }.map { f => Category(f._1, f._2) }.toList.sortBy(_.name)

    }
  }

  def getCategoryStrings : JsValue = {
    val cates = getCategories()
    var obj = Json.obj()
    for(x <- cates){
      obj = obj + (x.name -> Json.toJson(x.subs))
    }
    obj
  }
}

//need for listing engines
object Engine {
  def getEngines(): List[String] = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT DISTINCT client FROM reports
        """
      ).as(SqlParser.str("client").*)

    }
  }

  def getEnginesJS(): JsArray = {
    getEngines().foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
  }
}

//get the engine types
object EngineType {
  def getEngineTypes(): List[String] = {
    DB.withConnection { implicit connection =>

      val tmp = SQL(
        """SELECT DISTINCT type FROM reports
        """
      ).as(SqlParser.str("type").*)

      var result = new ListBuffer[String]()
      for(x<-tmp){
        if(x.contains(",")){
          x.split(",").foreach(y => result+=y.trim)
        }
        else{
          result+=x
        }
      }

      result.toList.distinct
    }
  }

  def getEngineTypesJS(): JsArray = {
    getEngineTypes().foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
  }

  def getEngineTypesFull(): List[String] = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT DISTINCT type FROM reports
        """
      ).as(SqlParser.str("type").*)

    }
  }
}

//get available language directions from table rules
object LangDirection {
  def getLangDirections(): List[String] = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT DISTINCT direction FROM rules order by direction
        """
      ).as(SqlParser.str("direction").*).map { x => x.substring(0, 2).toUpperCase + "->" + x.substring(2, 4).toUpperCase }

    }
  }
}

//report related
case class Report(id: Int, time: String)

object Report {

  implicit val peopleReader = Json.reads[Report]

  val simple = {
    get[Int]("id") ~
      get[Date]("time") map {
      //case id ~ time => Report(id, time.toString.replace(".0", ""))
      case id ~ time => Json.obj("id" -> id, "time" -> time.toString.replace(".0", ""))
    }
  }

  val full = {
    get[Int]("id") ~
      get[String]("client") ~
      get[String]("direction") ~
    get[String]("type") ~
    get[Int]("templateid") ~
    get[String]("comment") ~
      get[Date]("time") map {
      //case id ~ time => Report(id, time.toString.replace(".0", ""))
      case id ~ client ~ direction ~ engineType ~templateid ~comment~ time => Json.obj("id" -> id, "engine" -> client, "lang" -> direction, "type"->engineType, "templateid"->templateid, "comment"->comment, "time" -> time.toString.replace(".0", ""))
    }
  }

  def getLastReportID: Int = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT max(id) from reports
        """
      ).as(SqlParser.int("max(id)").single)

    }
  }

  def getTimeID(engine: String): JsArray = {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT id,time from reports where client='$engine' order by time desc
        """
      ).as(Report.simple *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))

    }
  }

  def getFull(): JsArray = {
    DB.withConnection { implicit connection =>
      SQL(
        s"""SELECT id,client,direction,time,type,templateid,comment from reports order by time desc
        """
      ).as(Report.full *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))

    }
  }

  def getTemplateIDs(): List[Int] = {
    DB.withConnection { implicit connection =>

      SQL(
        """SELECT DISTINCT templateid FROM reports
        """
      ).as(SqlParser.int("templateid").*)

    }
  }

  def deleteReport(id:String): JsValue = {
    DB.withConnection { implicit connection =>

      SQL(
        s"""delete FROM sentences where reportid=$id
        """
      ).executeUpdate()

      SQL(
        s"""delete FROM reports where id=$id
        """
      ).executeUpdate()

    }
    Json.toJson(0)
  }

  def updateComment(value: JsValue, reportid: JsValue) = {
    DB.withConnection { implicit connection =>
      val comment = value\"comment"
      val templateid = value\"templateid"
      Json.toJson(SQL(
        s"""update reports set comment='${comment.as[String]}', templateid=${templateid.as[String]} where id=$reportid"""
      ).executeUpdate())
    }
  }
}

object Rule {

  def updateRule(id: JsValue, direction: JsValue, pr: JsValue, nr: JsValue, pt: JsValue, nt: JsValue) = {
    DB.withConnection { implicit connection =>

      //recheck all sentences in other reports with the same id.
      val vid = id.as[String]
      val vdir = direction.as[String]
      val vpr = pr.as[String]
      val vnr = nr.as[String]
      val vpt = pt.as[String]
      val vnt = nt.as[String]

      val parser: RowParser[(Int,String,String, String)] = SqlParser.int("reportid") ~ SqlParser.str("sentenceid") ~ SqlParser.str("source") ~ SqlParser.str("translation") map (SqlParser.flatten)
      val res:List[(Int,String,String,String)] = SQL(
        s"""select reportid,sentenceid,source,translation from sentences,reports where direction='$vdir' and id=reportid and sentenceid='$vid'"""
      ).as(parser.*)

      val sentence = new Sentence(vid,vpt,vnt,vpr,vnr)
      for(result <- res){
        val newpass = controllers.Application.passCheck(result._4,sentence)

        //println("in updating rule:" + newpass+ " " + result._1+ " " + vid)

        SQL(s"""update sentences set pass=$newpass where reportid=${result._1} and sentenceid='$vid'""").executeUpdate()
      }


      //update rule table
      Json.toJson(SQL(
        s"""update rules set positiveTokens='${URLEncoder.encode(pt.as[String],"UTF-8") }',negativeTokens='${URLEncoder.encode(nt.as[String],"UTF-8")}',positiveRegex='${URLEncoder.encode(pr.as[String],"UTF-8")}',negativeRegex='${URLEncoder.encode(nr.as[String],"UTF-8")}' where ID='${id.as[String]}' and direction='${direction.as[String]}'""".replace("\\", "\\\\")
      ).executeUpdate())
    }
  }

  def insertNew(js: JsValue): JsValue = {
    DB.withConnection { implicit connection =>
      val id = (js\"id").as[String]
      val direction = (js\"direction").as[String]
      val category = (js\"category").as[String]
      val phe = (js\"phe").as[String]
      val pr = (js\"pr").as[String].replace("'","''")
      val nr = (js\"nr").as[String].replace("'","''")
      val source = (js\"source").as[String]
      val comment = (js\"comment").as[String]
      val preid = (js\"preid").as[String]
      val postid = (js\"postid").as[String]

      try{
        val res = SQL(s"""insert into rules values('$preid','$postid','$id','$direction','','$source','','$category','$phe','','$pr','','$nr','','')""").executeUpdate()
        Json.obj("result" -> res, "message" -> "")
      } catch {
        case pse: Exception =>
          println(pse.getMessage)
          return Json.obj("result"-> 2, "message" -> pse.getMessage)
      }

    }
  }
}

case class QTSystem(engine:String, engineType:String,datetime:String)

object QTSys{

  def toSelection(qTSystem: QTSystem): String = {
    qTSystem.engine + " (" + qTSystem.engineType + ")"
  }
}


case class Template(id:Int, engine: String, etype:String, datetime:String)
case class TemplateFull(id:Int, engine: String, etype:String, direction:String, templateid:Int)
object Template {

  val simple = {
    get[Int]("templateid") ~
      get[String]("client") ~
      get[String]("type") ~
      get[Date]("time") map {
      case id ~ name ~ introduced ~ time => Template(id,name,introduced,time.toString)
    }
  }

  val forResult = {
    get[Int]("id") ~
      get[String]("client") ~
      get[String]("type") ~
      get[String]("direction") ~
      get[Int]("templateid")  map {
      case id ~ name ~ introduced ~ lang ~ tid  => TemplateFull(id,name,introduced,lang,tid)
    }
  }

  def getTemplatesAndReports(): Map[Int, List[QTSystem]] = {

    DB.withConnection { implicit connection =>

      var states = scala.collection.mutable.Map[Int, List[QTSystem]]()

      SQL(
        s"""SELECT templateid,client,type,time from reports where templateid>0 order by id"""
      ).as(Template.simple *).foreach{ x=>
        if(states.keySet.contains(x.id)){
          val list = states.get(x.id).get
          states(x.id) = list :+ QTSystem(x.engine,x.etype,x.datetime)
        }
        else
          states += (x.id -> List(QTSystem(x.engine,x.etype,x.datetime)))
      }

      states.toMap
    }
  }

  def getComparisonResult(templateid:String) : JsArray = {

    DB.withConnection { implicit connection =>

      var finalarr = Json.arr()
      SQL(
        s"""SELECT id,client,type,direction,templateid from reports where templateid=$templateid order by id"""
      ).as(Template.forResult *).foreach{ x=>
        val res = SQL(
          s"""select ID,pass,positiveTokens,negativeTokens,positiveRegex,negativeRegex,sentences.source as source,translation,category,barrier,sentences.comment from rules, sentences where reportid=${x.id} and direction='${x.direction}' and rules.ID=sentences.sentenceid"""
        ).as(Sentence.checkResult *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))

        finalarr = finalarr :+ res
      }

      finalarr
    }
  }
}


//get certain amount of sentences in selected categories and barriers.
case class Sentence(id: String, positive: String, negative: String, positiveRegex: String, negativeRegex: String)

object Sentence {


  val simple = {
    get[String]("ID") ~
      get[String]("source") ~
      get[String]("category") ~
      get[String]("barrier") ~
      get[String]("comment") map {
      case id ~ name ~ introduced ~ discontinued ~ comment => Json.obj("id" -> id, "source" -> name, "category" -> introduced, "barrier" -> discontinued, "comment" -> comment)
    }
  }


  val forChecking = {
    get[String]("ID") ~
      get[String]("positiveTokens") ~
      get[String]("negativeTokens") ~
      get[String]("positiveRegex") ~
      get[String]("negativeRegex") map {
      case id ~ p ~ n ~ pr ~ nr => Sentence(id, URLDecoder.decode(p,"UTF-8"), URLDecoder.decode(n,"UTF-8"), URLDecoder.decode(pr,"UTF-8"), URLDecoder.decode(nr,"UTF-8"))
    }
  }

  def getSentenceForChecking(id: String, direction:String): Sentence = {
    DB.withConnection { implicit connection =>

      val res = SQL(
        s"""SELECT * from rules where id='$id' and direction='$direction'"""
      ).as(Sentence.forChecking *)

      if (res.size > 0) res(0)
      else Sentence("", "", "", "", "")
    }
  }

  val checkResult = {
    get[String]("ID") ~
      get[Int]("pass") ~
      get[String]("positiveTokens") ~
      get[String]("negativeTokens") ~
      get[String]("positiveRegex") ~
      get[String]("negativeRegex") ~
      get[String]("source") ~
      get[String]("translation") ~
      get[String]("category") ~
      get[String]("barrier") ~
      get[String]("comment") map {
      case id ~ pass ~ pt ~ nt ~ pr ~ nr ~ s ~ t ~ c ~ b ~ comment => Json.obj("id" -> id, "pass" -> {
        if (pass == 1) "<i class='glyphicon glyphicon-ok isBlocked' style='color:black'></i>"
        else if (pass == 2) "<i class='glyphicon glyphicon-remove isBlocked' style='color:black'></i>"
        else if (pass == 4) "<i class='glyphicon glyphicon-exclamation-sign isBlocked' style='color:black'></i>"
        else "<i class='glyphicon glyphicon-alert' style='color:black'></i>"
      }, "source" -> s, "category" -> c, "barrier" -> b,
        "translation" -> t, "positiveRegex" -> URLDecoder.decode(pr,"UTF-8"), "negativeRegex" -> URLDecoder.decode(nr,"UTF-8"), "positiveTokens" -> URLDecoder.decode(pt,"UTF-8"), "negativeTokens" -> URLDecoder.decode(nt,"UTF-8"), "comment" -> comment)
    }
  }

  def getCheckResult(id: Long, direction: String): JsArray = {
    DB.withConnection { implicit connection =>
      SQL(
        s"""select ID,pass,positiveTokens,negativeTokens,positiveRegex,negativeRegex,sentences.source as source,translation,category,barrier,sentences.comment from rules INNER JOIN sentences on rules.ID=sentences.sentenceid where reportid=$id and direction='$direction'"""
      ).as(Sentence.checkResult *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
    }
  }

  def getRandomSentenceFromCategory(num: JsValue, categories: JsValue, lang: JsValue): JsArray = {
    DB.withConnection { implicit connection =>

      val nums = num.as[String] match {
        case "" => "5" //default value on webpage
        case _ => num.as[String]
      }

      val cates = categories.toString match {
        case "null" => {
          //by default when nothing is selected: get all
          SQL(
            s"""SELECT distinct category from rules where direction='${lang.as[String]}'"""
          ).as(SqlParser.str("category").*)
        }
        case _ => categories.as[List[String]]
      }

      cates.flatMap(x => {

        var finalNum = nums
        if (nums.contains(".")) {
          val total = SQL(
            s"""SELECT count(*) as total from rules where category='$x' and direction='${lang.as[String]}'
        """
          ).as(SqlParser.int("total").single)
          finalNum = (total * nums.toFloat).toInt.toString
        }

        SQL(
          s"""SELECT * from rules where category='$x' and direction='${lang.as[String]}' ORDER BY rand() limit $finalNum
        """
        ).as(Sentence.simple *)
      }).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))


    }
  }

  def getRandomSentenceFromSub(num: JsValue, categories: JsValue, lang: JsValue): JsArray = {
    DB.withConnection { implicit connection =>

      val nums = num.as[String] match {
        case "" => "5"
        case _ => num.as[String]
      }

      val cates = categories.toString match {
        case "null" => {
          SQL(
            s"""SELECT distinct barrier from rules where direction='${lang.as[String]}'"""
          ).as(SqlParser.str("barrier").*)
        }
        case _ => categories.as[List[String]]
      }

      cates.flatMap(x => {

        var finalNum = nums
        if (nums.contains(".")) {
          val total = SQL(
            s"""SELECT count(*) as total from rules where barrier='$x' and direction='${lang.as[String]}'
        """
          ).as(SqlParser.int("total").single)
          finalNum = (total * nums.toFloat).toInt.toString
        }

        SQL(
          s"""SELECT * from rules where barrier='$x' and direction='${lang.as[String]}' ORDER BY rand() limit $finalNum
        """
        ).as(Sentence.simple *)
      }).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))


    }
  }

  def getRandomSentenceFromSubPerCategory(num: JsValue, categories: JsValue, lang: JsValue): JsArray = {
    DB.withConnection { implicit connection =>

      val nums = num.as[String] match {
        case "" => "5"
        case _ => num.as[String]
      }

      val cates = categories.toString match {
        case "null" => {
          SQL(
            s"""SELECT distinct category from rules where direction='${lang.as[String]}'"""
          ).as(SqlParser.str("category").*)
        }
        case _ => categories.as[List[String]]
      }

      val subs = SQL(
        s"""SELECT distinct barrier from rules where category in (${cates.mkString("'", "','", "'")}) and direction='${lang.as[String]}'
        """
      ).as(SqlParser.str("barrier").*)

      subs.flatMap(x => {

        var finalNum = nums
        if (nums.contains(".")) {
          val total = SQL(
            s"""SELECT count(*) as total from rules where barrier='$x' and direction='${lang.as[String]}'
        """
          ).as(SqlParser.int("total").single)
          finalNum = (total * nums.toFloat).toInt.toString
        }

        SQL(
          s"""SELECT * from rules where barrier='$x' and direction='${lang.as[String]}' ORDER BY rand() limit $finalNum
        """
        ).as(Sentence.simple *)
      }).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))


    }
  }

  def getSentenceLastReportPerEngine(engine: JsValue, lang: JsValue): JsArray = {
    DB.withConnection { implicit connection =>
      SQL(
        s"""select * from rules inner join sentences on ID=sentenceid where reportid= (select max(id) from reports where client='${engine.as[String]}' and direction='${lang.as[String]}') and rules.direction='${lang.as[String]}'"""
      ).as(Sentence.simple *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
    }
  }

  def getFailedSentencePerEngine(all: JsValue, engine: JsValue, lang: JsValue): JsArray = {
    DB.withConnection { implicit connection =>
      if (all.as[Boolean])
        SQL(
          s"""select ID,sentences.source as source,category,barrier from rules inner join sentences on ID=sentenceid where rules.direction='${lang.as[String]}' and (pass=2 or pass=3) and reportid in (select id from reports where client='${engine.as[String]}' and direction='${lang.as[String]}') group by ID"""
        ).as(Sentence.simple *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
      else
        SQL(
          s"""select * from rules inner join sentences on ID=sentenceid where (pass=2 or pass=3) and rules.direction='${lang.as[String]}' and reportid= (select max(id) from reports where client='${engine.as[String]}' and direction='${lang.as[String]}')"""
        ).as(Sentence.simple *).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
    }
  }

  def updatePass(report: JsValue, id: JsValue, pass: JsValue) = {

    DB.withConnection { implicit connection =>

      //println("in updating pass:" + pass + " " + report + " "+ id)
      Json.toJson(SQL(
        s"""update sentences set pass=$pass where reportid=$report and sentenceid='${id.as[String]}'"""
      ).executeUpdate())
    }
  }

  def updateComment(value: JsValue, reportid: JsValue, id: JsValue) = {
    DB.withConnection { implicit connection =>
      Json.toJson(SQL(
        s"""update sentences set comment='${value.as[String]}' where reportid=$reportid and sentenceid='${id.as[String]}'"""
      ).executeUpdate())
    }
  }
}



