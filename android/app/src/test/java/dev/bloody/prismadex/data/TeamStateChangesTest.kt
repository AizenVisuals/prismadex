package dev.bloody.prismadex.data
import org.junit.Assert.*
import org.junit.Test
class TeamStateChangesTest {
 @Test fun deletingCardTeamClearsReferenceAndPreservesOtherData(){
  val a=SavedTeam("a","A");val b=SavedTeam("b","B")
  val state=UserState(teams=listOf(a,b),activeTeamId="a",trainerCardTeamId="b",favorites=setOf(25),trainerName="Red")
  val next=state.withoutTeam("b")
  assertEquals(listOf(a),next.teams);assertEquals("",next.trainerCardTeamId);assertEquals(a,next.cardTeam)
  assertEquals(state.favorites,next.favorites);assertEquals("Red",next.trainerName)
  assertEquals("b",state.withoutTeam("a").activeTeamId)
  assertEquals(state,state.withoutTeam("missing"));assertEquals(next,next.withoutTeam("a"))
 }
}
