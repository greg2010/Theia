package org.red.db.models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Theia extends {
  val profile = org.red.SlickCodegen.CustomPostgresDriver
} with Theia

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Theia {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = NpcKillData.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table NpcKillData
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param systemId Database column system_id SqlType(int8)
   *  @param npcKills Database column npc_kills SqlType(int8)
   *  @param fromTstamp Database column from_tstamp SqlType(timestamptz)
   *  @param toTstamp Database column to_tstamp SqlType(timestamptz)
   *  @param source Database column source SqlType(text) */
  case class NpcKillDataRow(id: Int, systemId: Long, npcKills: Long, fromTstamp: java.sql.Timestamp, toTstamp: java.sql.Timestamp, source: String)
  /** GetResult implicit for fetching NpcKillDataRow objects using plain SQL queries */
  implicit def GetResultNpcKillDataRow(implicit e0: GR[Int], e1: GR[Long], e2: GR[java.sql.Timestamp], e3: GR[String]): GR[NpcKillDataRow] = GR{
    prs => import prs._
    NpcKillDataRow.tupled((<<[Int], <<[Long], <<[Long], <<[java.sql.Timestamp], <<[java.sql.Timestamp], <<[String]))
  }
  /** Table description of table npc_kill_data. Objects of this class serve as prototypes for rows in queries. */
  class NpcKillData(_tableTag: Tag) extends profile.api.Table[NpcKillDataRow](_tableTag, "npc_kill_data") {
    def * = (id, systemId, npcKills, fromTstamp, toTstamp, source) <> (NpcKillDataRow.tupled, NpcKillDataRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(systemId), Rep.Some(npcKills), Rep.Some(fromTstamp), Rep.Some(toTstamp), Rep.Some(source)).shaped.<>({r=>import r._; _1.map(_=> NpcKillDataRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column system_id SqlType(int8) */
    val systemId: Rep[Long] = column[Long]("system_id")
    /** Database column npc_kills SqlType(int8) */
    val npcKills: Rep[Long] = column[Long]("npc_kills")
    /** Database column from_tstamp SqlType(timestamptz) */
    val fromTstamp: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("from_tstamp")
    /** Database column to_tstamp SqlType(timestamptz) */
    val toTstamp: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("to_tstamp")
    /** Database column source SqlType(text) */
    val source: Rep[String] = column[String]("source")

    /** Uniqueness Index over (systemId,fromTstamp,toTstamp,source) (database name npc_kill_data_system_id_from_tstamp_to_tstamp_source_key) */
    val index1 = index("npc_kill_data_system_id_from_tstamp_to_tstamp_source_key", (systemId, fromTstamp, toTstamp, source), unique=true)
  }
  /** Collection-like TableQuery object for table NpcKillData */
  lazy val NpcKillData = new TableQuery(tag => new NpcKillData(tag))
}
