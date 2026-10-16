package com.dante.sportshuub

import java.text.Normalizer

object Rivalries {

    private data class Rivalry(
        val label: String,
        val teamA: String,
        val teamB: String
    )

    // Names are matched against ESPN's team names (case and accents ignored).
    // Pro teams use city + name and college teams include the mascot, so
    // "Michigan Wolverines" doesn't also match "Michigan State Spartans".
    // Add your own lines at the bottom.
    private val rivalries = listOf(
        // Brazil / South America
        Rivalry("Fla-Flu", "Flamengo", "Fluminense"),
        Rivalry("Clássico dos Milhões", "Flamengo", "Vasco"),
        Rivalry("Derby Paulista", "Corinthians", "Palmeiras"),
        Rivalry("Majestoso", "Corinthians", "Sao Paulo"),
        Rivalry("Gre-Nal", "Gremio", "Internacional"),
        Rivalry("Clássico Mineiro", "Atletico", "Cruzeiro"),
        Rivalry("Superclásico", "Boca Juniors", "River Plate"),

        // Spain
        Rivalry("El Clásico", "Real Madrid", "Barcelona"),
        Rivalry("Madrid Derby", "Real Madrid", "Atletico Madrid"),
        Rivalry("Seville Derby", "Sevilla", "Real Betis"),

        // England
        Rivalry("Manchester Derby", "Manchester City", "Manchester United"),
        Rivalry("North London Derby", "Arsenal", "Tottenham"),
        Rivalry("Liverpool vs Man United", "Liverpool", "Manchester United"),
        Rivalry("Merseyside Derby", "Liverpool", "Everton"),

        // Italy / Germany / France
        Rivalry("Derby d'Italia", "Inter Milan", "Juventus"),
        Rivalry("Milan Derby", "Inter Milan", "AC Milan"),
        Rivalry("Rome Derby", "Roma", "Lazio"),
        Rivalry("Der Klassiker", "Bayern Munich", "Borussia Dortmund"),
        Rivalry("Le Classique", "Paris Saint-Germain", "Marseille"),

        // MLB
        Rivalry("Red Sox vs Yankees", "Boston Red Sox", "New York Yankees"),
        Rivalry("Dodgers vs Giants", "Los Angeles Dodgers", "San Francisco Giants"),
        Rivalry("Cubs vs Cardinals", "Chicago Cubs", "St. Louis Cardinals"),
        Rivalry("Subway Series", "New York Mets", "New York Yankees"),

        // NFL
        Rivalry("Cowboys vs Eagles", "Dallas Cowboys", "Philadelphia Eagles"),
        Rivalry("Packers vs Bears", "Green Bay Packers", "Chicago Bears"),
        Rivalry("Steelers vs Ravens", "Pittsburgh Steelers", "Baltimore Ravens"),
        Rivalry("Chiefs vs Raiders", "Kansas City Chiefs", "Las Vegas Raiders"),

        // NBA
        Rivalry("Lakers vs Celtics", "Los Angeles Lakers", "Boston Celtics"),
        Rivalry("Knicks vs Celtics", "New York Knicks", "Boston Celtics"),
        Rivalry("Lakers vs Clippers", "Los Angeles Lakers", "LA Clippers"),

        // NHL
        Rivalry("Bruins vs Canadiens", "Boston Bruins", "Montreal Canadiens"),
        Rivalry("Rangers vs Islanders", "New York Rangers", "New York Islanders"),
        Rivalry("Leafs vs Canadiens", "Toronto Maple Leafs", "Montreal Canadiens"),

        // College football
        Rivalry("Iron Bowl", "Alabama Crimson Tide", "Auburn Tigers"),
        Rivalry("The Game", "Ohio State Buckeyes", "Michigan Wolverines"),
        Rivalry("Red River Rivalry", "Texas Longhorns", "Oklahoma Sooners"),
        Rivalry("Florida-Georgia", "Georgia Bulldogs", "Florida Gators"),
        Rivalry("Florida-Florida State", "Florida Gators", "Florida State Seminoles"),
        Rivalry("Alabama-LSU", "Alabama Crimson Tide", "LSU Tigers"),
        Rivalry("Third Saturday in October", "Alabama Crimson Tide", "Tennessee Volunteers"),
        Rivalry("Notre Dame-USC", "Notre Dame Fighting Irish", "USC Trojans"),
        Rivalry("Army-Navy", "Army Black Knights", "Navy Midshipmen"),

        // College basketball
        Rivalry("Duke-UNC", "Duke Blue Devils", "North Carolina Tar Heels"),
        Rivalry("Kentucky-Louisville", "Kentucky Wildcats", "Louisville Cardinals")
    )

    // Returns the rivalry's name if this game is one, otherwise null.
    fun find(game: EspnGame): String? {
        val home = normalize(game.home.name)
        val away = normalize(game.away.name)

        for (r in rivalries) {
            val a = normalize(r.teamA)
            val b = normalize(r.teamB)

            if ((home.contains(a) && away.contains(b)) ||
                (home.contains(b) && away.contains(a))
            ) {
                return r.label
            }
        }
        return null
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
