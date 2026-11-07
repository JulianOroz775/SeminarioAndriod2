package ar.edu.unicen.seminario.repository

import ar.edu.unicen.seminario.model.Movie
import ar.edu.unicen.seminario.model.MovieDetail
import ar.edu.unicen.seminario.model.MoviePage
import ar.edu.unicen.seminario.network.MovieApi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class MovieRepository(
    private val movieApi: MovieApi,
    private val token: String
) {

    suspend fun getPopularMovies(
        language: String = "es-AR",
        page: Int = 1
    ): Result<MoviePage> {
        return try {
            val response = movieApi.getPopularMovies(
                token = "Bearer $token",
                language = language,
                page = page
            )

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(
                        IllegalStateException("Empty popular movies response")
                    )

                Result.success(
                    MoviePage(
                        movies = body.results,
                        totalPages = body.total_pages
                    )
                )
            } else {
                Result.failure(
                    HttpException(response)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieDetails(
        movieId: Int,
        language: String = "es-AR"
    ): Result<MovieDetail> {
        return try {
            val response = movieApi.getMovieDetails(
                movieId = movieId,
                token = "Bearer $token",
                language = language
            )

            if (response.isSuccessful) {
                val detail = response.body()
                    ?: return Result.failure(
                        IllegalStateException("Empty movie details response")
                    )

                Result.success(detail)
            } else {
                Result.failure(
                    HttpException(response)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMovies(
        query: String,
        language: String = "es-AR"
    ): Result<List<Movie>> {
        return try {
            val response = movieApi.searchMovies(
                query = query,
                token = "Bearer $token",
                language = language
            )

            if (response.isSuccessful) {
                val movies = response.body()?.results
                    ?: return Result.failure(
                        IllegalStateException("Empty search response")
                    )

                Result.success(movies)
            } else {
                Result.failure(
                    HttpException(response)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
