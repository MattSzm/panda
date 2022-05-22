package com.github.mattszm.panda.user

import com.github.mattszm.panda.user.token.Token
import monix.connect.mongodb.client.{CollectionCodecRef, MongoConnection}
import org.mongodb.scala.{ConnectionString, MongoClientSettings}
import org.scalacheck.Gen
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName


trait UserTokenFixture {
  private val dbName = "test"
  protected val mongoContainer: MongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo")
    .withTag("4.0.10"))
  mongoContainer.start()

  private val settings: MongoClientSettings =
    MongoClientSettings.builder()
      .applyToClusterSettings(builder => {
        builder.applyConnectionString(new ConnectionString(mongoContainer.getReplicaSetUrl(dbName)))
        ()
      }
      ).build()

  private val usersCol: CollectionCodecRef[User] = User.getCollection(dbName)
  private val tokensCol: CollectionCodecRef[Token] = Token.getCollection(dbName)

  private val usersWithTokensConnection = MongoConnection.create2(settings, (usersCol, tokensCol))


  private val userDao: UserDao = new UserDaoImpl(usersWithTokensConnection)
  protected val userService: UserService = new UserServiceImpl(userDao)(usersWithTokensConnection)

  def randomString(prefix: String): String = Gen.uuid.map(prefix + _.toString.take(15)).sample.get
}
