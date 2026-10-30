package dev.bloody.prismadex.data

/** Delete a team without leaving dangling active/card references or removing the last team. */
fun UserState.withoutTeam(id:String):UserState {
 val remaining=teams.filterNot{it.id==id}
 if(remaining.isEmpty()||remaining.size==teams.size)return this
 return copy(teams=remaining,activeTeamId=activeTeamId.takeIf{active->remaining.any{it.id==active}}?:remaining.first().id,
  trainerCardTeamId=trainerCardTeamId.takeUnless{it==id}.orEmpty())
}
