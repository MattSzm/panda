package com.github.pandafolks.panda.bootstrap

import com.github.pandafolks.panda.bootstrap.configuration.AppConfiguration
import com.github.pandafolks.panda.db.DbAppClient
import com.github.pandafolks.panda.nodestracker.{NodeTrackerDao, NodeTrackerDaoImpl, NodeTrackerService, NodeTrackerServiceImpl}
import com.github.pandafolks.panda.participant.event.{ParticipantEventDao, ParticipantEventDaoImpl, ParticipantEventService, ParticipantEventServiceImpl}
import com.github.pandafolks.panda.user.token.{TokenService, TokenServiceImpl}
import com.github.pandafolks.panda.user.{UserDao, UserDaoImpl, UserService, UserServiceImpl}
import com.pandafolks.mattszm.panda.sequence.SequenceDao

final class DaoAndServiceInitialization(
                                         private val dbAppClient: DbAppClient,
                                         private val appConfiguration: AppConfiguration,
                                       ) {

  private val userDao: UserDao = new UserDaoImpl(dbAppClient.getUsersWithTokensConnection)
  private val userService: UserService = new UserServiceImpl(userDao, List(appConfiguration.initUser))(dbAppClient.getUsersWithTokensConnection)

  private val tokenService: TokenService = new TokenServiceImpl(appConfiguration.authTokens)(dbAppClient.getUsersWithTokensConnection)

  private val sequenceDao: SequenceDao = new SequenceDao()

  private val participantEventDao: ParticipantEventDao = new ParticipantEventDaoImpl(dbAppClient.getParticipantEventsAndSequencesConnection)
  private val participantEventService: ParticipantEventService = new ParticipantEventServiceImpl(
    participantEventDao = participantEventDao,
    sequenceDao = sequenceDao
  )(dbAppClient.getParticipantEventsAndSequencesConnection)

  private val nodeTrackerDao: NodeTrackerDao = new NodeTrackerDaoImpl(dbAppClient.getNodesConnection)
  private val nodeTrackerService: NodeTrackerService = new NodeTrackerServiceImpl(nodeTrackerDao)(appConfiguration.consistency.fullConsistencyMaxDelay)

  def getUserService: UserService = userService

  def getTokenService: TokenService = tokenService

  def getParticipantEventService: ParticipantEventService = participantEventService

  def getNodeTrackerService: NodeTrackerService = nodeTrackerService

}
