package com.yunjae.imageCheck.config

import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

object HibernateUtil {
  private val sessionFactory = buildSessionFactory

  private def buildSessionFactory: SessionFactory = {
    try
      // Create the SessionFactory from hibernate.cfg.xml
      return new Configuration ().configure("hibernate.cfg.xml").buildSessionFactory

    catch {
      case ex: Throwable => {
        // Make sure you log the exception, as it might be swallowed
        System.err.println("Initial SessionFactory creation failed." + ex);
        throw new ExceptionInInitializerError(ex);
      }
    }
  }

  def getSessionFactory = sessionFactory


  def shutdown {
    // Close caches and connection pools
    getSessionFactory.close();
  }
}