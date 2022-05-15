package com.github.mattszm.panda.user

import cats.data.OptionT
import cats.effect.Resource
import cats.implicits.toTraverseOps
import com.github.mattszm.panda.participant.event.ParticipantEvent
import com.github.mattszm.panda.sequence.Sequence
import com.github.mattszm.panda.user.token.Token
import com.github.mattszm.panda.utils.{AlreadyExists, PersistenceError}
import monix.connect.mongodb.client.CollectionOperator
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import tsec.passwordhashers.jca.BCrypt

import java.util.UUID
import scala.concurrent.duration.DurationInt

final class UserServiceImpl(private val userDao: UserDao, private val initUsers: List[UserCredentials])(
  private val c: Resource[Task, (CollectionOperator[User], CollectionOperator[ParticipantEvent],
    CollectionOperator[Sequence], CollectionOperator[Token])]) extends UserService {

  locally {
    (for {
      empty <- userDao.checkIfEmpty
      _ <- if (empty)
        initUsers
          .map(u => create(u.username, u.password))
          .sequence
      else Task.unit
    } yield ())
      .onErrorRecover { _ => () }
      .delayExecution(5.seconds)
      .runAsyncAndForget
  }

  override def getById(id: UserId): Task[Option[User]] = userDao.byId(id)

  override def checkPassword(credentials: UserCredentials): Task[Option[User]] = userDao.validateUser(credentials)

  override def delete(credentials: UserCredentials): Task[Boolean] =
    c.use {
      case (userOperator, _, _, _) => OptionT(userDao.validateUser(credentials, userOperator))
        .foldF(Task.now(false))(userDao.delete(_, userOperator))
    }

  override def create(username: String, password: String): Task[Either[PersistenceError, Unit]] =
    c.use {
      case (userOperator, _, _, _) =>
        for {
          id <- Task.now(UUID.randomUUID()).map(tagUUIDAsUserId)
          pwd <- BCrypt.hashpw[Task](password)
          exits <- userDao.exists(username, userOperator)
          result <- if (!exits)
            userDao.insertOne(User(id, username, pwd), userOperator)
          else
            Task.now(Left(AlreadyExists("User with the username \"" + username + "\" already exists")))
        } yield result
    }
}