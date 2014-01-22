package es.upm.fi.oeg.siq.wrapper

import java.util.Date
import scala.io.Source
import java.io._
import dispatch._
import com.ning.http.client.RequestBuilder
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import sys.process._

class CsvSource(who:PollWrapper,id:String) extends Datasource(who,id){      
  val idname=who.idkeys(id)
  //val the url=who.url.replace("{id}",id)
  var theurl=who.url.replace("{id}",id)
  lazy val rowrate=who.configvals("rowrate").toLong
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
		print(data)
		val writer = new PrintWriter(new File("conf/data/hsl.csv"), "UTF-8")
	    writer.write(data)
	    writer.close()
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
    println(array.mkString(" "))
    var i=1
    array.map{value=>
      //i+=1
      fieldTypes(i)(value)
    }    
  }
}