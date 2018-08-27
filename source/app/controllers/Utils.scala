package controllers

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by Renlong.Ai@dfki.de on 01.03.17.
  */
object Utils {

  def unscrambleList(path:String,template:String):List[(String,String,String)] = {

    val fileContents = Source.fromFile(path).getLines().toList
    var result = new ListBuffer[(String,String,String)]()

    case class Unscrambling(id: String, pos: Int, source: String)
    val forChecking = {
      get[String]("ID") ~
        get[Int]("pos") ~
        get[String]("source")  map {
        case id ~ p ~ n  => Unscrambling(id, p, n)
      }
    }

    DB.withConnection { implicit connection =>
      val res = SQL(
        s"""select rules.ID,pos,rules.source from templates INNER JOIN rules where templates.id=$template and rules.ID=templates.sentence and rules.direction=templates.lang"""
      ).as(forChecking *)

      for(x <- res){
        result += ( (x.id,x.source,fileContents(x.pos)) )
      }

    }
    result.toList
  }

  def generateScrambledList(input:Map[String,String],lang:String,factor:Float) : (Int,List[String]) = {
    var output = List()
    val num = (input.size * factor).toInt
    val language = lang.substring(0,2)

    DB.withConnection { implicit connection =>

      var res = SQL(
        s"""SELECT text from distractors where lang='$language' order by rand() limit $num"""
      ).as(SqlParser.str("text") *)

      //res.foreach(x=>println(x))

      val r = scala.util.Random
      //positions of the true sentences in the scrambled list
      //use number of actual sentences plus number of distractor sentences for the total limit
      var locations = Map[String,Int]()
      for ((id,v)<-input){
        var pos = r.nextInt(num+input.size)
        //need to make sure no duplicate positions are assigned
        while (locations.values.toSeq.contains(pos)) pos = r.nextInt(num+input.size)
        locations+=(id->pos)
      }
      //sort first so that it won't overflow when inserting
      val locs = ListMap(locations.toSeq.sortBy(_._2):_*)
      //println(locs)
      //won't start at 0 to reveal how many reports we had already
      var last = 256
      val count = SQL(s"""select count(*) as max from templates""").as(SqlParser.int("max").single)
      if(count>0) last = SQL(s"""select max(id) as max from templates""").as(SqlParser.int("max").single)
      val newid = last+1

      //insert sentences to selected distractors, which is already shuffled
      for((id,i) <- locs){
        val (front, back) = res.splitAt(i)
        res = front ++ List(input.get(id).get) ++ back
        //println(input.get(id).get)
        SQL(s"""insert into templates values ($newid,'$id',$i,'$lang')""").execute()
      }

      //res.foreach(x=>println(x))
      (newid,res)
    }


  }

}
