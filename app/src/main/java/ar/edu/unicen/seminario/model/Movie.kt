package ar.edu.unicen.seminario.model

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val vote_average: Double,
    val genre_ids: List<Int> = emptyList()
)

