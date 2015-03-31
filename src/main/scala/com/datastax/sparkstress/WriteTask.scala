package com.datastax.sparkstress

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import com.datastax.sparkstress.RowTypes._
import com.datastax.spark.connector._

object WriteTask {
  val ValidTasks = Set(
    "writeshortrow",
    "writeperfrow",
    "writewiderow",
    "writerandomwiderow"
  )
}

abstract class WriteTask( var config: Config, val sc: SparkContext) extends StressTask {

  def setupCQL() = {
    CassandraConnector(sc.getConf).withSessionDo{ session =>
      if (config.deleteKeyspace){
      println("Destroying Keyspace")
      session.execute(s"DROP KEYSPACE IF EXISTS ${config.keyspace}")
      }
      val kscql = getKeyspaceCql(config.keyspace)
      val tbcql = getTableCql(config.table)
      println(s"Running the following create statements\n$kscql\n$tbcql")
      session.execute(kscql)
      session.execute(s"USE ${config.keyspace}")
      session.execute(tbcql)
    }
    printf("Done Setting up CQL Keyspace/Table\n")
  }

  def getKeyspaceCql(ksName: String): String = s"CREATE KEYSPACE IF NOT EXISTS $ksName WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 }"

  def getTableCql(tbName: String): String

  def run() : Unit

  def setConfig(c: Config): Unit  = {
    config = c
  }

  def runTrials(sc: SparkContext): Seq[Long] = {
    println("About to Start Trials")
    for (trial <- 1 to config.trials) yield {setupCQL(); time(run())}
  }


}


class WriteShortRow(config: Config, sc: SparkContext) extends WriteTask(config, sc) {

  def getTableCql(tbName: String): String =
    s"""CREATE TABLE IF NOT EXISTS $tbName
       |(key bigint, col1 text, col2 text, col3 text, PRIMARY KEY(key))""".stripMargin

  def getRDD: RDD[ShortRowClass] =
    RowGenerator.getShortRowRDD(sc, config.numPartitions, config.totalOps)

  def run(): Unit = {
    setupCQL()
    getRDD.saveToCassandra(config.keyspace, config.table)
  }
}

class WritePerfRow(config: Config, sc: SparkContext) extends WriteTask(config, sc) {

  def getTableCql(tbName: String): String =
    s"""CREATE TABLE IF NOT EXISTS $tbName
       |(key text, size text, qty int, time timestamp, color text,
       |col1 text, col2 text, col3 text, col4 text, col5 text,
       |col6 text, col7 text, col8 text, col9 text, col10text,
       |PRIMARY KEY (key))}
     """.stripMargin

  def getRDD: RDD[PerfRowClass] =
    RowGenerator.getPerfRowRdd(sc, config.numPartitions, config.totalOps)

  def run(): Unit = {
    setupCQL()
    getRDD.saveToCassandra(config.keyspace, config.table)
  }

}

class WriteWideRow(config: Config, sc: SparkContext) extends WriteTask( config, sc){

  def getTableCql(tbName: String): String =
    s"""CREATE TABLE IF NOT EXISTS $tbName
      |(key int, col1 text, col2 text, col3 text,
      |PRIMARY KEY (key, col1))
    """.stripMargin

  def getRDD: RDD[ShortRowClass] =
    RowGenerator
      .getWideRowRdd(sc, config.numPartitions, config.totalOps, config.numTotalKeys)

  def run(): Unit = {
    setupCQL()
    getRDD.saveToCassandra(config.keyspace, config.table)
  }
}

class WriteRandomWideRow(config: Config, sc: SparkContext) extends WriteTask(config, sc){

  def getTableCql(tbName: String): String =
    s"""CREATE TABLE IF NOT EXISTS $tbName
       |(key int, col1 text, col2 text, col3 text,
       |PRIMARY KEY (key, col1))
     """.stripMargin

  def getRDD[T]: RDD[ShortRowClass] =
    RowGenerator.getRandomWideRow(sc, config.numPartitions, config.totalOps, config.numTotalKeys)

  def run(): Unit = {
    setupCQL()
    getRDD.saveToCassandra(config.keyspace, config.table)
  }

}