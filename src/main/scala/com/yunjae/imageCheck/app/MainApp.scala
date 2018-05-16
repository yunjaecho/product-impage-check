package com.yunjae.imageCheck.app

import java.sql.{Connection, PreparedStatement}
import java.util
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import com.yunjae.imageCheck.config.HibernateUtil
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.Session
import scalaj.http.{Http, HttpOptions}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object MainApp extends App {

  var session: Session = null

  //var conn: Connection = null

  val executionContext = ExecutionContext

  val pool = Executors.newFixedThreadPool(10)
  implicit val ec = ExecutionContext.fromExecutor(pool)

  var count: AtomicInteger = new AtomicInteger(0)

  val imageList = getImageList()

  imageList
    .forEach(path => getUrlStatus(path))

  /**
    * 상품 이미지 데이터 조회
    * @return
    */
  def getImageList() = {

    val selectSql = s"""
                       |  SELECT
                       |        CONTENT_FILE_ID,
                       |        SAVED_PATH,
                       |        GD_NM,
                       |        CONTAINER_CATEGORY,
                       |        GD_MST_NO
                       |  FROM (
                       |		SELECT
                       |		      B.CONTENT_FILE_ID,
                       |          B.SAVED_PATH,
                       |		      B.CONTAINER_CATEGORY,
                       |		      A.GD_MST_NO,
                       |          A.GD_NM,
                       |		      RANK() OVER(PARTITION BY A.GD_MST_NO, B.CONTAINER_CATEGORY ORDER BY B.CONTENT_FILE_ID DESC) RANK_NO
                       |		FROM  TB_GOODS        A,
                       |		      TB_CONTENT_FILE B
                       |		WHERE A.GD_MST_NO = B.CONTAINER_ID
                       |		  AND A.GD_STATE_CD = 'G0101'
                       |		  AND A.USE_YN = 'Y'
                       |		  AND SYSDATE BETWEEN STA_DT AND END_DT
                       |		  AND DEEL_MODEL_CD = 'G1101'
                       |      AND B.SAVED_PATH IS NOT NULL
                       |	   )
                       |  WHERE RANK_NO = 1
                       |  ORDER BY TO_NUMBER(CONTENT_FILE_ID) DESC """.stripMargin


    var list: util.List[_] = null


    try {
      session = HibernateUtil.getSessionFactory.openSession()
      //conn = (session.asInstanceOf[SessionImplementor]).getJdbcConnectionAccess.obtainConnection()

      val sqlQuery = session.createSQLQuery(selectSql)
      list = sqlQuery.list
    } catch {
      case ex: Exception =>
        println(ex.getStackTrace)
    } finally  {
      //session.close
      //HibernateUtil.shutdown
    }

    list
  }

  /**
    * 이미지 존재여부 Http Status(200) 확인
    * @param value
    * @return
    */
  def getUrlStatus(value: Any): Future[Unit] = {
    Future {
      val data = castTuple(value)

      val savePath = data._2
      HttpOptions.connTimeout(1000)
      val response = Http(s"https://www.pos-mall.co.kr/upload/$savePath").asString

      if (response.code != 200) {
        println(data._1  + "\t" + data._2 + "\t" + data._3 + "\t" + data._4+ "\t" + data._5)

        val updateSql = s"UPDATE TB_GOODS SET GD_STATE_CD ='G0106' , UP_NO = 99999 , UP_DT = SYSDATE WHERE GD_MST_NO = '${data._5}'"
        println(updateSql)

        session.doWork(conn => {
          conn.prepareStatement(updateSql).executeUpdate()
          conn.commit()
        })

      }

      // Executors shutdown
      if(count.incrementAndGet() == imageList.size()) {
        pool.shutdown()

        session.close
        HibernateUtil.shutdown
      }


    }
  }


  def futureTest(value: Any): Unit = {
    val timeout = 3.seconds
    val data = castTuple(value)
    val savePath = String.valueOf(data._2)

    val status: Future[Unit] = Future {
      println(Thread.currentThread().getName)
      val response = Http(s"https://www.pos-mall.co.kr/upload/$savePath").asString
      println(response.code)
      if (response.code != 200) {
        val category = savePath.substring(0, savePath.indexOf("/"))
        val gdMstNo = savePath.substring(savePath.indexOf("/")+ 1, savePath.indexOf("."))
        println(category + "\t" + gdMstNo)
      }
    }

    Await.result(status, timeout)
  }

  /**
    * Any Type cast Tuple2
    * @param value
    * @return
    */
  def castTuple(value: Any): (String, String, String, String, String) = {
    value match {
      case Array(a, b, c, d, e) =>
        (String.valueOf(a), String.valueOf(b), String.valueOf(c), String.valueOf(d), String.valueOf(e))
    }
  }


}


