package com.github.mattszm.panda.participant

import com.github.mattszm.panda.routes.Group
import com.github.mattszm.panda.utils.TestScheduler.scheduler
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers.{be, contain}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class ParticipantsCacheImplTest extends AsyncFlatSpec {
  private def createCache(): ParticipantsCacheImpl =
    new ParticipantsCacheImpl(
      List(
        Participant("59.145.84.51", 4001, Group("cars"), "id1"),
        Participant("59.145.84.52", 4001, Group("cars"), "id2"),
        Participant("59.145.84.53", 4002, Group("planes"), "id3")
      )
    )

  "ParticipantsCacheImpl#getParticipantsAssociatedWithGroup" should "return appropriate results for the requested group" in {
    val cache: ParticipantsCache = createCache()

    cache.getParticipantsAssociatedWithGroup(Group("cars")).toList should
      contain theSameElementsAs List(
      Participant("59.145.84.51", 4001, Group("cars"), "id1"),
      Participant("59.145.84.52", 4001, Group("cars"), "id2")
    )
  }

  it should "return empty vector if there are no elements associated with the group" in {
    val cache: ParticipantsCache = createCache()
    cache.getParticipantsAssociatedWithGroup(Group("whatever")).size should be(0)
  }

  "ParticipantsCacheImpl#addParticipant" should "be able to add an element if the group already exists" in {
    val cache: ParticipantsCache = createCache()
    cache.addParticipant(Participant("59.145.84.54", 4402, Group("planes"), "id4")).runToFuture
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("planes")) should
          contain theSameElementsAs List(
          Participant("59.145.84.53", 4002, Group("planes"), "id3"),
          Participant("59.145.84.54", 4402, Group("planes"), "id4")
        )
      )
  }

  it should "be able to add an element and create the group if the one does not exist" in {
    val cache: ParticipantsCache = createCache()
    cache.addParticipant(Participant("59.145.86.59", 4405, Group("ships"), "id11")).runToFuture
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("ships")) should
          contain theSameElementsAs List(Participant("59.145.86.59", 4405, Group("ships"), "id11"))
      )
  }

  "ParticipantsCacheImpl#addParticipants" should "be able to add multiple elements at once" in {
    val cache: ParticipantsCache = createCache()
    cache.addParticipants(List(
      Participant("59.145.84.54", 4402, Group("planes"), "id4"),
      Participant("59.126.84.56", 4402, Group("planes"), "id5"),
      Participant("59.145.86.59", 4405, Group("ships"), "id11")
    )).runToFuture
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("planes")) should
          contain theSameElementsAs List(
          Participant("59.145.84.53", 4002, Group("planes"), "id3"),
          Participant("59.145.84.54", 4402, Group("planes"), "id4"),
          Participant("59.126.84.56", 4402, Group("planes"), "id5")
        )
      )
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("ships")) should
          contain theSameElementsAs List(Participant("59.145.86.59", 4405, Group("ships"), "id11"))
      )
  }

  "ParticipantsCacheImpl#removeParticipant" should "remove requested participant" in {
    val cache: ParticipantsCache = createCache()
    cache.removeParticipant(Participant("59.145.84.51", 4001, Group("cars"), "id1")).runToFuture
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("cars")) should
          contain theSameElementsAs List(
          Participant("59.145.84.52", 4001, Group("cars"), "id2")
        )
      )
  }

  it should "not remove anything if the group exists but the specific value does not" in {
    val cache: ParticipantsCache = createCache()
    cache.removeParticipant(Participant("59.145.84.61", 4001, Group("cars"), "id1")).runToFuture //different ip
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("cars")) should
          contain theSameElementsAs List(
            Participant("59.145.84.51", 4001, Group("cars"), "id1"),
            Participant("59.145.84.52", 4001, Group("cars"), "id2"),
        )
      )
  }

  it should "not remove anything if the group does not exist" in {
    val cache: ParticipantsCache = createCache()
    cache.removeParticipant(Participant("59.145.84.61", 4001, Group("different"), "id1")).runToFuture
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("cars")) should
          contain theSameElementsAs List(
          Participant("59.145.84.51", 4001, Group("cars"), "id1"),
          Participant("59.145.84.52", 4001, Group("cars"), "id2"),
        )
      )
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("planes")) should
          contain theSameElementsAs List(
          Participant("59.145.84.53", 4002, Group("planes"), "id3")
        )
      )
  }

  "ParticipantsCacheImpl#removeAllParticipantsAssociatedWithGroup" should "remove all values associated with the group" in {
    val cache: ParticipantsCache = createCache()
    cache.removeAllParticipantsAssociatedWithGroup(Group("cars")).runToFuture
      .map(res => res should be(Right(())))
      .map(_ => cache.getParticipantsAssociatedWithGroup(Group("cars")).size should be(0))
  }

  it should "not remove anything if the group does not exist in the cache" in {
    val cache: ParticipantsCache = createCache()
    cache.removeAllParticipantsAssociatedWithGroup(Group("different")).runToFuture
      .map(res => res should be(Right(())))
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("cars")) should
          contain theSameElementsAs List(
          Participant("59.145.84.51", 4001, Group("cars"), "id1"),
          Participant("59.145.84.52", 4001, Group("cars"), "id2"),
        )
      )
      .map(_ =>
        cache.getParticipantsAssociatedWithGroup(Group("planes")) should
          contain theSameElementsAs List(
          Participant("59.145.84.53", 4002, Group("planes"), "id3")
        )
      )
  }
}
