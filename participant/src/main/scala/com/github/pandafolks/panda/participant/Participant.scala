package com.github.pandafolks.panda.participant

import com.github.pandafolks.panda.routes.Group
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import monix.eval.Task
import org.http4s.EntityEncoder
import org.http4s.Uri.Path
import org.http4s.circe.jsonEncoderOf


final case class Participant(
                              host: String,
                              port: Int,
                              group: Group,
                              identifier: String,
                              heartbeatInfo: HeartbeatInfo,
                              status: ParticipantStatus,
                              health: ParticipantHealth = Unhealthy // Participant will become healthy after first successful health check
                            ) {
  def isWorking: Boolean = status == Working

  def isHealthy: Boolean = health == Healthy
}

object Participant {
  final val HEARTBEAT_DEFAULT_ROUTE: String = "heartbeat"

  def apply(host: String, port: Int, group: Group): Participant =
    new Participant(
      host = host,
      port = port,
      group = group,
      identifier = createDefaultIdentifier(host, port, group.name),
      heartbeatInfo = HeartbeatInfo(HEARTBEAT_DEFAULT_ROUTE),
      status = Working,
    )

  def apply(host: String, port: Int, group: Group, identifier: String): Participant =
    new Participant(
      host = host,
      port = port,
      group = group,
      identifier = identifier,
      heartbeatInfo = HeartbeatInfo(HEARTBEAT_DEFAULT_ROUTE),
      status = Working,
    )

  def apply(host: String, port: Int, group: Group, heartbeatInfo: HeartbeatInfo): Participant =
    new Participant(
      host = host,
      port = port,
      group = group,
      identifier = createDefaultIdentifier(host, port, group.name),
      heartbeatInfo = heartbeatInfo,
      status = Working
    )

  def createDefaultIdentifier(host: String, port: Int, groupName: String): String =
    List(host, port, groupName).mkString("-")

  @Deprecated
  def createHeartbeatEndpoint(host: String, port: Int, route: String): String =
      Path.unsafeFromString(Path.unsafeFromString(host).dropEndsWithSlash.renderString + ":" + port)
        .concat(Path.unsafeFromString(route))
        .renderString

  implicit val participantEncoder: EntityEncoder[Task, Participant] = jsonEncoderOf[Task, Participant]
  implicit val participantSeqEncoder: EntityEncoder[Task, Seq[Participant]] = jsonEncoderOf[Task, Seq[Participant]]
}
