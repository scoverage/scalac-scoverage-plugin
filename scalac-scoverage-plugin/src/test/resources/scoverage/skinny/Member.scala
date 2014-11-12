package scoverage.report.scoverage.skinny

import skinny.orm._
import scalikejdbc._, SQLInterpolation._
import scala.language.dynamics

// If your model has +23 fields, switch this to normal class and mixin scalikejdbc.EntityEquality.
case class Member(id: Long, name: String)

object Member extends SkinnyCRUDMapper[Member] {
  override lazy val tableName = "members"
  override lazy val defaultAlias = createAlias("m")

  override def extract(rs: WrappedResultSet, rn: ResultName[Member]): Member = new Member(
    id = rs.get(rn.id),
    name = rs.get(rn.name)
  )
}

