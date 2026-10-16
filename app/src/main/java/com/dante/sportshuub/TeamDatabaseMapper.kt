package com.dante.sportshuub

fun Team.toEntity(): TeamEntity {
    return TeamEntity(
        id = id,
        name = name,
        abbreviation = abbreviation,
        sport = sport,
        league = league,
        logoFileName = logoFileName,
        logoUrl = logoUrl
    )
}

fun TeamEntity.toTeam(): Team {
    return Team(
        id = id,
        name = name,
        abbreviation = abbreviation,
        sport = sport,
        league = league,
        logoFileName = logoFileName,
        logoUrl = logoUrl
    )
}