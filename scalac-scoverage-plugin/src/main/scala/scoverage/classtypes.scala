package scoverage

/** @author Stephen Samuel */
sealed trait ClassType
object ClassType {
  case object Object extends ClassType
  case object Class extends ClassType
  case object Trait extends ClassType
}