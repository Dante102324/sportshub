package com.dante.sportshuub

object TeamRepository {

    private val teams = mutableMapOf<String, Team>()

    @Volatile
    private var databaseTeams = mutableMapOf<String, Team>()

    // =====================================================
    // DEFAULT TEAM DATA
    // =====================================================

    init {

        // =====================================================
        // NBA TEAMS
        // =====================================================

        addTeam(
            Team(
                id = "nba_atlanta_hawks",
                name = "Atlanta Hawks",
                abbreviation = "ATL",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "hawks_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/atl.png"
            )
        )

        addTeam(
            Team(
                id = "nba_boston_celtics",
                name = "Boston Celtics",
                abbreviation = "BOS",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "celtics_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/bos.png"
            )
        )

        addTeam(
            Team(
                id = "nba_brooklyn_nets",
                name = "Brooklyn Nets",
                abbreviation = "BKN",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "nets_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/bkn.png"
            )
        )

        addTeam(
            Team(
                id = "nba_charlotte_hornets",
                name = "Charlotte Hornets",
                abbreviation = "CHA",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "hornets_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/cha.png"
            )
        )

        addTeam(
            Team(
                id = "nba_chicago_bulls",
                name = "Chicago Bulls",
                abbreviation = "CHI",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "bulls_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/chi.png"
            )
        )

        addTeam(
            Team(
                id = "nba_cleveland_cavaliers",
                name = "Cleveland Cavaliers",
                abbreviation = "CLE",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "cavaliers_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/cle.png"
            )
        )

        addTeam(
            Team(
                id = "nba_dallas_mavericks",
                name = "Dallas Mavericks",
                abbreviation = "DAL",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "mavericks_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/dal.png"
            )
        )

        addTeam(
            Team(
                id = "nba_denver_nuggets",
                name = "Denver Nuggets",
                abbreviation = "DEN",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "nuggets_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/den.png"
            )
        )

        addTeam(
            Team(
                id = "nba_detroit_pistons",
                name = "Detroit Pistons",
                abbreviation = "DET",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "pistons_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/det.png"
            )
        )

        addTeam(
            Team(
                id = "nba_golden_state_warriors",
                name = "Golden State Warriors",
                abbreviation = "GSW",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "warriors_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/gsw.png"
            )
        )

        addTeam(
            Team(
                id = "nba_houston_rockets",
                name = "Houston Rockets",
                abbreviation = "HOU",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "rockets_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/hou.png"
            )
        )

        addTeam(
            Team(
                id = "nba_indiana_pacers",
                name = "Indiana Pacers",
                abbreviation = "IND",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "pacers_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/ind.png"
            )
        )

        addTeam(
            Team(
                id = "nba_la_clippers",
                name = "Los Angeles Clippers",
                abbreviation = "LAC",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "clippers_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/lac.png"
            )
        )

        addTeam(
            Team(
                id = "nba_los_angeles_lakers",
                name = "Los Angeles Lakers",
                abbreviation = "LAL",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "lakers_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/lal.png"
            )
        )

        addTeam(
            Team(
                id = "nba_memphis_grizzlies",
                name = "Memphis Grizzlies",
                abbreviation = "MEM",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "grizzlies_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/mem.png"
            )
        )

        addTeam(
            Team(
                id = "nba_miami_heat",
                name = "Miami Heat",
                abbreviation = "MIA",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "heat_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/mia.png"
            )
        )

        addTeam(
            Team(
                id = "nba_milwaukee_bucks",
                name = "Milwaukee Bucks",
                abbreviation = "MIL",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "bucks_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/mil.png"
            )
        )

        addTeam(
            Team(
                id = "nba_minnesota_timberwolves",
                name = "Minnesota Timberwolves",
                abbreviation = "MIN",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "timberwolves_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/min.png"
            )
        )

        addTeam(
            Team(
                id = "nba_new_orleans_pelicans",
                name = "New Orleans Pelicans",
                abbreviation = "NOP",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "pelicans_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/no.png"
            )
        )

        addTeam(
            Team(
                id = "nba_new_york_knicks",
                name = "New York Knicks",
                abbreviation = "NYK",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "knicks_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/ny.png"
            )
        )

        addTeam(
            Team(
                id = "nba_oklahoma_city_thunder",
                name = "Oklahoma City Thunder",
                abbreviation = "OKC",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "thunder_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/okc.png"
            )
        )

        addTeam(
            Team(
                id = "nba_orlando_magic",
                name = "Orlando Magic",
                abbreviation = "ORL",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "magic_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/orl.png"
            )
        )

        addTeam(
            Team(
                id = "nba_philadelphia_76ers",
                name = "Philadelphia 76ers",
                abbreviation = "PHI",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "76ers_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/phi.png"
            )
        )

        addTeam(
            Team(
                id = "nba_phoenix_suns",
                name = "Phoenix Suns",
                abbreviation = "PHX",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "suns_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/phx.png"
            )
        )

        addTeam(
            Team(
                id = "nba_portland_trail_blazers",
                name = "Portland Trail Blazers",
                abbreviation = "POR",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "trailblazers_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/por.png"
            )
        )

        addTeam(
            Team(
                id = "nba_sacramento_kings",
                name = "Sacramento Kings",
                abbreviation = "SAC",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "kings_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/sac.png"
            )
        )

        addTeam(
            Team(
                id = "nba_san_antonio_spurs",
                name = "San Antonio Spurs",
                abbreviation = "SAS",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "spurs_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/sas.png"
            )
        )

        addTeam(
            Team(
                id = "nba_toronto_raptors",
                name = "Toronto Raptors",
                abbreviation = "TOR",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "raptors_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/tor.png"
            )
        )

        addTeam(
            Team(
                id = "nba_utah_jazz",
                name = "Utah Jazz",
                abbreviation = "UTA",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "jazz_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/utah.png"
            )
        )

        addTeam(
            Team(
                id = "nba_washington_wizards",
                name = "Washington Wizards",
                abbreviation = "WAS",
                sport = "Basketball",
                league = "NBA",
                logoFileName = "wizards_logo",
                logoUrl = "https://a.espncdn.com/i/teamlogos/nba/500/wsh.png"
            )
        )
    }

    // =====================================================
    // DATABASE
    // =====================================================

    fun initializeDatabase(context: android.content.Context) {

        Thread {

            try {

                val database =
                    SportsHubDatabase.getInstance(context)

                val existingTeams =
                    database.teamDao().getAllTeams()

                if (existingTeams.isEmpty()) {

                    val defaultTeams =
                        teams.values.map { team ->

                            TeamEntity(
                                id = team.id,
                                name = team.name,
                                abbreviation = team.abbreviation,
                                sport = team.sport,
                                league = team.league,
                                logoFileName = team.logoFileName,
                                logoUrl = team.logoUrl
                            )
                        }

                    database.teamDao().insertTeams(defaultTeams)

                    databaseTeams =
                        teams.toMutableMap()

                    android.util.Log.d(
                        "TeamRepository",
                        "Inserted ${defaultTeams.size} teams into database"
                    )

                } else {

                    val convertedTeams =
                        existingTeams.associateBy {

                            "${it.league}_${it.abbreviation}"
                                .uppercase()
                        }

                    databaseTeams =
                        convertedTeams.mapValues {
                            it.value.toTeam()
                        }.toMutableMap()

                    android.util.Log.d(
                        "TeamRepository",
                        "Database already contains ${databaseTeams.size} teams"
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "TeamRepository",
                    "Failed to initialize database",
                    e
                )
            }

        }.start()
    }

    // =====================================================
    // ADD TEAM
    // =====================================================

    fun addTeam(team: Team) {

        val key =
            "${team.league}_${team.abbreviation}".uppercase()

        teams[key] = team
    }

    // =====================================================
    // GET TEAM
    // =====================================================

    fun getTeam(
        league: String,
        abbreviation: String
    ): Team? {

        val key =
            "${league}_${abbreviation}".uppercase()

        return databaseTeams[key] ?: teams[key]
    }

    // =====================================================
    // GET ALL TEAMS
    // =====================================================

    fun getAllTeams(): List<Team> {

        return if (databaseTeams.isNotEmpty()) {
            databaseTeams.values.toList()
        } else {
            teams.values.toList()
        }
    }
    fun refreshDatabase(context: android.content.Context, onComplete: (Boolean, String) -> Unit) {
        Thread {
            try {
                val database = SportsHubDatabase.getInstance(context)
                val dao = database.teamDao()

                val defaultTeams = teams.values.map { team ->
                    TeamEntity(
                        id = team.id,
                        name = team.name,
                        abbreviation = team.abbreviation,
                        sport = team.sport,
                        league = team.league,
                        logoFileName = team.logoFileName,
                        logoUrl = team.logoUrl
                    )
                }

                dao.insertTeams(defaultTeams)

                val savedTeams = dao.getAllTeams()
                val updatedCache = savedTeams.associateBy {
                    "${it.league}_${it.abbreviation}".uppercase()
                }.mapValues {
                    it.value.toTeam()
                }.toMutableMap()

                databaseTeams = updatedCache

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onComplete(
                        true,
                        "Database refreshed: ${updatedCache.size} teams loaded"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "TeamRepository",
                    "Database refresh failed",
                    e
                )

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onComplete(
                        false,
                        "Database refresh failed: ${e.message ?: "Unknown error"}"
                    )
                }
            }
        }.start()
    }
}