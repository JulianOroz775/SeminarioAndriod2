package ar.edu.unicen.seminario

import android.content.Context
import ar.edu.unicen.seminario.model.Movie
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WishlistManager(context: Context) {

    private val preferences = context.getSharedPreferences(
        "movie_wishlist",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    fun getWishlist(): Set<Int> {
        return preferences.getStringSet("movie_ids", emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    fun getSavedMovies(): List<Movie> {
        val json = preferences.getString("saved_movies", null)
            ?: return emptyList()

        return try {
            val type = object : TypeToken<List<Movie>>() {}.type
            gson.fromJson<List<Movie>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isFavorite(movieId: Int): Boolean {
        return getWishlist().contains(movieId)
    }

    fun toggleFavorite(movie: Movie): Boolean {
        val updatedIds = getWishlist().toMutableSet()
        val savedMovies = getSavedMovies()
            .associateBy { it.id }
            .toMutableMap()

        val isFavorite = if (updatedIds.contains(movie.id)) {
            updatedIds.remove(movie.id)
            savedMovies.remove(movie.id)
            false
        } else {
            updatedIds.add(movie.id)
            savedMovies[movie.id] = movie
            true
        }

        preferences.edit()
            .putStringSet(
                "movie_ids",
                updatedIds.map { it.toString() }.toSet()
            )
            .putString(
                "saved_movies",
                gson.toJson(savedMovies.values.toList())
            )
            .apply()

        return isFavorite
    }
}
