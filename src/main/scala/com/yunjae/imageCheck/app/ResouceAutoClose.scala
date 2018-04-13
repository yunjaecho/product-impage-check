package com.yunjae.imageCheck.app

import java.io.Closeable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object TryWith {
  def apply [C <: Closeable, R](resGen: => C) (r: Closeable => R): Try[R] =
    Try(resGen).flatMap(closeable => {
      try {
        Success(r(closeable))
      }
      catch {
        case NonFatal(e) => Failure(e)
      }
      finally {
        try {
          closeable.close()
        }
        catch {
          case e: Exception =>
            System.err.println("Failed to close Resource:")
            e.printStackTrace()
        }
      }
    })
}