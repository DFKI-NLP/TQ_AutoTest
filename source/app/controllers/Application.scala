package controllers

import java.io.{File, FileInputStream, FileWriter}
import java.util.Date
import java.util.regex.PatternSyntaxException

import anorm.SqlParser.get
import models._
import play.api.db.DB
import play.api.libs.json.{JsArray, JsValue, Json, Reads}
import play.api.mvc.{Action, Controller}
import anorm._
import play.api.Play.current
import com.github.tototoshi.csv._

import scala.collection.mutable.ListBuffer

object Application extends Controller {

  var path = current.configuration.getString("path.temp").get

  var time = java.util.Calendar.getInstance().getTime.toString

  //pages
  def index = Action {
    Ok(views.html.dashboard())
  }

  def rules  = Action {
    Ok(views.html.rules())
  }

  def report = Action {
    Ok(views.html.report())
  }

  def regex = Action {
    Ok(views.html.regex())
  }

  def dataPreparation = Action {
    Ok(views.html.preparation())
  }

  def loadData = Action {
    Ok(views.html.upload())
  }

  def compare = Action {
    Ok(views.html.compare())
  }

  def dbmanagement = Action {
    Ok(views.html.dbmanagement())
  }

  def newSentence = Action {
    Ok(views.html.newsentence())
  }

  def getDetail(source:String,translation:String,pr:String,nr:String,pt:String,nt:String,sentenceid:String) = Action {
    Ok(views.html.detail(source,translation,pr,nr,pt,nt,sentenceid))
  }

  //fake check return either 1 or 2 (pass or fail)
  def fakeCheck(input:String,sentence:Sentence): Int = {
    scala.util.Random.nextInt(2)+1
  }

  def passCheck(input:String,sentence:Sentence): Int = {
//    println(input)
//    println(sentence)
    if(input.length==0 || sentence.id=="")
      return 9 //either sentence is empty or id not valid, in this case it won't be processed.

    var positive = false
    var negative = false

    //no new line allowed in regex.
    if((sentence.positive + sentence.positiveRegex + sentence.negative + sentence.negativeRegex).contains('\n'))
      return 4

    //need to catch PatternSyntaxException because the regex input could be wrong [Vivien's mistake ;)]
    try {
      //check positive
      if (sentence.positiveRegex.length > 0 || sentence.negativeRegex.length > 0) {
        if (sentence.positiveRegex.length == 0)
          positive = false
        else
          positive = sentence.positiveRegex.r.findFirstIn(input) match {
            case None => false
            case Some(_) => true
          }
        if (sentence.negativeRegex.length == 0)
          negative = false
        else
          negative = sentence.negativeRegex.r.findFirstIn(input) match {
            case None => false
            case Some(_) => true
          }
      }
      if (!positive && !negative) {
        if (sentence.positive.length == 0)
          positive = false
        else
          for (x <- sentence.positive.split("\\|")) {
            if (input.contains(x.trim))
              positive = true
          }
        if (sentence.negative.length == 0)
          negative = false
        else
          for (x <- sentence.negative.split("\\|")) {
            if (input.contains(x.trim))
              negative = true
          }
      }
    } catch {
      case pse: PatternSyntaxException => return 4
    }

    //println("positive: "+positive + "; negative: "+negative + "; sentence: "+ input+"; pr: "+sentence.positiveRegex+ "; nr: "+sentence.negativeRegex)

    if(positive && !negative)
      1 //positive
    else if(!positive && negative)
      2 //negative
    else
      3 //warning: either found both or none
  }

  //upload
  def upload = Action(parse.multipartFormData) { request =>

    val engine = request.body.dataParts.get("engine").get.mkString
    val direction = request.body.dataParts.get("direction").get.mkString.replace("->","").toLowerCase
    val engineType = request.body.dataParts.get("type").get.mkString
    val template = request.body.dataParts.get("template").get.mkString
    val comment = request.body.dataParts.get("comment").get.mkString

    //insert report and get id
    var id = 0l
    DB.withConnection { implicit connection =>

      id = SQL(
        s"""insert into reports (direction,client,type,templateid,comment) values ('$direction','$engine','$engineType',$template,'$comment')
        """
      ).executeInsert().asInstanceOf[Option[Long]].get

    }

    def escape (input:String) = input.replace("\'","\'\'")

    request.body.file("report").map { report =>
      import java.io.File
      report.ref.moveTo(new File(path+"lastUpload"),true)

      //val content = Utils.unscrambleList(path+"lastUpload",template)
      //handle old report, in which case template id is asserted to 0
      val content = template match {
        case "0" =>
          val bufferedSource = io.Source.fromFile(path+"lastUpload")
          var result = new ListBuffer[(String,String,String)]()
          for (line <- bufferedSource.getLines) {
            val cols = line.split("\t").map(_.trim)
            result += ((cols(0),cols(1),cols(2)))
          }
          bufferedSource.close
          result
        case _ => Utils.unscrambleList(path+"lastUpload",template)
      }

      content.map {x =>
        val sentence = Sentence.getSentenceForChecking(x._1,direction)

        //val pass = passCheck(x._3,sentence)
        val pass = passCheck(x._3,sentence)
        if(pass<9) DB.withConnection { implicit connection =>
          SQL( s"""insert into sentences values ($id,'${sentence.id}','${escape(x._2)}','${escape(x._3)}',$pass,'')""").execute()
        }
        pass
      }

      Ok(Sentence.getCheckResult(id,direction))
    }.getOrElse {
      Redirect(routes.Application.upload()).flashing(
        "error" -> "Missing file")
    }
  }

  //ajax
  def getSentenceFromCategoryWithNum = Action {

    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.getRandomSentenceFromCategory(json\"num",json\"categories",json\"lang"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def getSentenceFromSubWithNum = Action {

    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.getRandomSentenceFromSub(json\"num",json\"categories",json\"lang"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def getSentenceFromSubPerCategory = Action {

    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.getRandomSentenceFromSubPerCategory(json\"num",json\"categories",json\"lang"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def getSentenceFromLastReportOfEngine = Action {
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.getSentenceLastReportPerEngine(json\"engine",json\"lang"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def getFailedSentences = Action {
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.getFailedSentencePerEngine(json\"all",json\"engine",json\"lang"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  //get the whole report of a given id
  def getReportResult = Action{
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.getCheckResult((json\"id").toString().toLong,(json\"direction").as[String]))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def deleteReport = Action {
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Report.deleteReport((json\"id").toString()))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  //update pass status of given sentence (sentence id with report id)
  def updatePass = Action{
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.updatePass(json\"report",json\"id",json\"pass"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def updateRules = Action{
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Rule.updateRule(json\"id",json\"direction",json\"pr",json\"nr",json\"pt",json\"nt"))
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  //update comment of single sentence
  def updateSentenceComment = Action{
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Sentence.updateComment(json\"value",json\"reportid",json\"id"))

      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def updateReportComment = Action{
    implicit  request=>{
      request.body.asJson.map{
        json =>
          Ok(Report.updateComment(json\"value",json\"reportid"))

      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }


  case class ExportedSentence (id:String,source:String,category:String,barrier:String,comment:String)
  object ExportedSentence {
    implicit val userReads: Reads[ExportedSentence] = Json.reads[ExportedSentence]
  }

  def scrambleList = Action {

    implicit  request=>{

      var output:Map[String,String] = Map()

      request.body.asFormUrlEncoded.get.get("data").map{
        json => {
          val jres = Json.parse(json(0))
          (jres \ "data").validate[List[ExportedSentence]].get.foreach{
            x => output+=(x.id -> x.source)
          }
          val (id,list) = controllers.Utils.generateScrambledList(output,(jres\"lang").as[String], (jres\"factor").as[Float])
          //println(list)
          val filename = s"$path/report$id.txt"
          val fw = new FileWriter(filename, true)
          try {
            for(line <- list){
              fw.write(line.trim+"\n")
            }
          } finally {
            fw.close()
          }
          //add to template meta:
          DB.withConnection { implicit connection =>
            SQL(s"""insert into template_meta values ($id,'${(jres\"meta").as[String] + ". [scramble factor:" + (jres\"factor").as[Float]}]')""").execute()
          }
          Ok.sendFile(new File(filename))
        }
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def getComparisonData = Action  {
    implicit  request=>{
      request.body.asJson.map{
        json =>{
          val templateid = (json\"id").as[String]
          val result = Template.getComparisonResult(templateid)
          Ok(result)
        }
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def updateTokens = Action {
    implicit  request=>{
      request.body.asJson.map{
        json =>{
          val direction = (json\"direction").as[String]
          val tokens = (json\"tokens").as[Map[String,String]]
          for((id,v) <- tokens){
            val pn = v.substring(0,1) match {
              case "p" => "positiveTokens"
              case "n" => "negativeTokens"
            }
            var token = v.substring(1)
            DB.withConnection { implicit connection =>

              val oldToken = SQL(s"""select $pn from rules where id='$id' and direction='$direction'""").as(SqlParser.str(pn).single)
              if(oldToken.length>0){
                if(!oldToken.contains(token))
                  token = oldToken + "|" + token
                else
                  token = oldToken
              }

              SQL( s"""update rules set $pn='${token.replace("'","''")}' where id='$id' and direction='$direction'""").execute()

              //also need to recheck all related sentences in other reports.
              val parser: RowParser[(Int,String,String, String,String,String,String,String)] = SqlParser.int("reportid") ~ SqlParser.str("sentenceid") ~ SqlParser.str("source") ~ SqlParser.str("translation") ~ SqlParser.str("positiveTokens") ~ SqlParser.str("positiveRegex") ~ SqlParser.str("negativeTokens") ~ SqlParser.str("negativeRegex") map (SqlParser.flatten)
              val res:List[(Int,String,String,String,String,String,String,String)] = SQL(
                s"""select reportid,sentenceid,rules.source,translation,positiveTokens,positiveRegex,negativeTokens,negativeRegex from sentences,reports,rules where rules.direction='$direction' and reports.direction='$direction' and reports.id=reportid and sentenceid='$id' and sentenceid=rules.ID"""
              ).as(parser.*)

              for(result <- res){
                val sentence = new Sentence(result._2,result._5,result._7,result._6,result._8)
                val newpass = controllers.Application.passCheck(result._3,sentence)

                SQL(s"""update sentences set pass=$newpass where reportid=${result._1} and sentenceid='$id'""").executeUpdate()
              }
            }
          }
          Ok(json)
        }
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def getSampleSentences() = Action {
    Ok(views.html.sampleSentences())
  }

  def insertRule() = Action {
    implicit  request=>{
      request.body.asJson.map{
        json =>{
          val result = Rule.insertNew(json)
//          println("insert result: " + result)
          Ok(result)
        }
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def recheck() = Action {

    var affected=0

    DB.withConnection { implicit connection =>

      val parser: RowParser[(Int,String,String, String,String,String,String,String,Int)] = SqlParser.int("reportid") ~ SqlParser.str("sentenceid") ~ SqlParser.str("source") ~ SqlParser.str("translation") ~ SqlParser.str("positiveTokens") ~ SqlParser.str("positiveRegex") ~ SqlParser.str("negativeTokens") ~ SqlParser.str("negativeRegex") ~ SqlParser.int("pass") map (SqlParser.flatten)
      val res:List[(Int,String,String,String,String,String,String,String,Int)] = SQL(
        """select reportid,sentenceid,sentences.source,translation,positiveRegex,positiveTokens,negativeRegex,negativeTokens,pass from sentences,rules,reports where reports.id=sentences.reportid and rules.ID=sentenceid and rules.direction=reports.direction"""
      ).as(parser.*)

      for(x<-res){
        val input = x._4
        val sentence = Sentence(x._2,x._5,x._7,x._6,x._8)
        val pass = passCheck(input,sentence)
        if(pass != x._9){
          affected+=1
          SQL(s"""update sentences set pass=$pass where reportid=${x._1} and sentenceid='${x._2}'""").executeUpdate()
          //println("affected: " + x._3)
        }
      }

      //println("affected sentences: " + affected)

    }
    Ok(Json.obj("affected"->affected))
  }

  def queryCount() = Action {
    implicit  request=>{
      request.body.asJson.map{
        json =>{
          DB.withConnection { implicit connection =>
            try{
              val full = {
                get[Int]("pass") ~ get[String]("reports.client") ~ get[String]("reports.type") ~ get[String]("reports.direction") ~ get[Date]("time") ~ get[String]("rules.source") ~ get[String]("translation") ~ get[String]("category") ~ get[String]("barrier")  map {
                  //case id ~ time => Report(id, time.toString.replace(".0", ""))
                  case pass ~ client ~ engineType ~ direction  ~time ~source~ translation~category~barrier => Json.obj("pass" -> {
                    if (pass == 1) "<i class='glyphicon glyphicon-ok isBlocked' style='color:black'></i>"
                    else if (pass == 2) "<i class='glyphicon glyphicon-remove isBlocked' style='color:black'></i>"
                    else if (pass == 4) "<i class='glyphicon glyphicon-exclamation-sign isBlocked' style='color:black'></i>"
                    else "<i class='glyphicon glyphicon-alert' style='color:black'></i>"
                  }, "client" -> client,"type"->engineType, "lang" -> direction, "time" -> time.toString.replace(".0", ""),"source"->source,"translation"->translation,"category"->category,"barrier"->barrier)
                }
              }
              println(s"""select reports.client,reports.type,reports.direction,time,rules.source,translation,category,barrier,pass from reports,sentences,rules where reports.id=sentences.reportid and sentenceid=rules.ID and rules.direction=reports.direction and ${(json\"where").as[String]}""")
              val res = SQL(s"""select reports.client,reports.type,reports.direction,time,rules.source,translation,category,barrier,pass from reports,sentences,rules where reports.id=sentences.reportid and sentenceid=rules.ID and rules.direction=reports.direction and ${(json\"where").as[String]}""").as(full.*).foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
              Ok(Json.obj("result"->res,"message"->""))
            } catch {
              case pse: Exception =>
                println(pse.getMessage)
                Ok(Json.obj("result"-> -1, "message" -> pse.getMessage))
            }
          }
        }
      }.getOrElse {
        BadRequest("Expecting application/json request body")
      }
    }

  }

}