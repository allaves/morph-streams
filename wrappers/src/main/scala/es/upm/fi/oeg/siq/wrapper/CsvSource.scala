package es.upm.fi.oeg.siq.wrapper

import java.util.Date
import scala.io.Source
import dispatch._
import com.ning.http.client.RequestBuilder
import sys.process._
import javax.annotation.Resource
import java.nio.file._

class CsvSource(who:PollWrapper,id:String) extends Datasource(who,id){
  val idname=who.idkeys(id)
  //val the url=who.url.replace("{id}",id)
  var theurl=who.url.replace("{id}",id)
  // Stream push rate
  var rowrate=who.configvals("rowrate").toLong
  lazy val values=who.configvals("values").split(',').map{v=>new Func(v)}
  val funs = values.map{v=>v.instantiate}

  val data = {
	  /*
	   * Extension of the CSV wrappers for online resources
	   */ 
	  if (theurl.startsWith("http://")) {
	    val svc = url(theurl)
		val res = Http(svc OK as.String)
		val data = res()
		
		val hslPath = Paths.get("conf/data/hsl.csv")
		if (!Files.exists(hslPath)) {
		  Files.createFile(hslPath)
		}
		Files.write(hslPath, data.getBytes("UTF-8"))
		//val writer = new PrintWriter(new File(), "UTF-8")
	    //writer.write(data)
	    //writer.close()
		theurl = "data/hsl.csv"
	  }
	  /*
	   * 
	   */
	  val d = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(theurl)).bufferedReader
	  val dat=Stream.continually(d.readLine()).takeWhile(_!=null).map(extract(_))
	  //d.close
	  dat
  }
  
  override def afterPosting={
    if (rowrate>0) Thread.sleep(rowrate)
  }
  
  override def pollData={
    val date=new Date
    val res = data     
    res.map{data=>
      new Observation(date,Seq(idname)++data++funs.map(f=>f(id)))
    }   
  }
    
  def extract(string:String)={
    printf("Line: %s\n", string)
    val array=string.split(";", -1)
    // Demo total time
//    lazy val demoTime = who.configvals("demoTime").toLong
//    if (demoTime != null) {
//      println(array(2).toLong)
//      //rowrate = rowRateCalculate(demoTime, 0, array(1).toLong, 31536000000L)
//      rowrate = (demoTime * (array(2).toLong - 0)) / (31536000000L - 0)
//      println(rowrate)
//    }
    println(array.mkString(" "))
    var i=1
    array.map{value=>
      //i+=1
      fieldTypes(i)(value)
    }    
  }
  
  // Calculated line by line the time to push the next line
//  def rowRateCalculate(demoTime:Long, t0:Long, ti:Long, tn:Long) {
//	val partialTime = ti - t0
//	val totalTime = tn - t0
//	rowrate = (demoTime * partialTime) / totalTime
//  }
}