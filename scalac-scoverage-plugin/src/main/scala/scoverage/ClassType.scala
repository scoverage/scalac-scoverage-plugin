package scoverage

object ClassType {
  case object Object extends ClassType
  case object Class extends ClassType
  case object Trait extends ClassType
}

/** @author Stephen Samuel */
sealed trait ClassType