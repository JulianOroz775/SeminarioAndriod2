package ar.edu.unicen.seminario

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import ar.edu.unicen.seminario.model.Movie
import ar.edu.unicen.seminario.network.RetrofitInstance
import ar.edu.unicen.seminario.repository.MovieRepository
import ar.edu.unicen.seminario.viewmodel.MovieDetailViewModel
import ar.edu.unicen.seminario.viewmodel.MovieDetailViewModelFactory

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: MovieDetailViewModel
    private var movieId: Int = -1

    private lateinit var wishlistManager: WishlistManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)
        wishlistManager = WishlistManager(this)
        val buttonWishlist = findViewById<Button>(R.id.buttonWishlist)

        movieId = intent.getIntExtra("movie_id", -1)

        if (movieId == -1) {
            finish()
            return
        }

        fun updateWishlistButton() {
            buttonWishlist.text = if (wishlistManager.isFavorite(movieId)) {
                getString(R.string.remove_from_wishlist)
            } else {
                getString(R.string.add_to_wishlist)
            }

            buttonWishlist.contentDescription =
                getString(R.string.wishlist_button_accessibility)
        }

        updateWishlistButton()

        buttonWishlist.setOnClickListener {
            val movie = viewModel.movie.value

            if (movie != null) {
                val savedMovie = Movie(
                    id = movie.id,
                    title = movie.title,
                    overview = movie.overview,
                    poster_path = movie.poster_path,
                    vote_average = movie.vote_average,
                    genre_ids = movie.genres.map { it.id }
                )

                wishlistManager.toggleFavorite(savedMovie)
                updateWishlistButton()
            }
        }

        val imagePoster = findViewById<ImageView>(R.id.imageDetailPoster)
        val textTitle = findViewById<TextView>(R.id.textDetailTitle)
        val textRating = findViewById<TextView>(R.id.textDetailRating)
        val textGenres = findViewById<TextView>(R.id.textDetailGenres)
        val textOverview = findViewById<TextView>(R.id.textDetailOverview)
        val progress = findViewById<ProgressBar>(R.id.progressDetailLoading)
        val layoutError = findViewById<View>(R.id.layoutDetailError)
        val textError = findViewById<TextView>(R.id.textDetailError)
        val buttonRetry = findViewById<Button>(R.id.buttonDetailRetry)

        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN
        val repository = MovieRepository(RetrofitInstance.movieApi, token)
        val factory = MovieDetailViewModelFactory(repository)

        viewModel = ViewModelProvider(this, factory)
            .get(MovieDetailViewModel::class.java)

        viewModel.movie.observe(this) { movie ->
            if (movie != null) {
                textTitle.text = movie.title
                imagePoster.contentDescription = getString(
                    R.string.movie_poster_description,
                    movie.title
                )

                textRating.text = getString(
                    R.string.movie_rating,
                    String.format(
                        java.util.Locale.getDefault(),
                        "%.1f",
                        movie.vote_average
                    )
                )

                textGenres.text = getString(R.string.movie_genres_label) + " " +
                        movie.genres.joinToString(", ") { it.name }

                textOverview.text = movie.overview.ifBlank {
                    getString(R.string.no_overview)
                }

                val posterUrl = movie.poster_path?.let {
                    "https://image.tmdb.org/t/p/w500$it"
                }

                Glide.with(this)
                    .load(posterUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imagePoster)

                layoutError.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                textError.text = when (error) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> getString(R.string.network_error)

                    is java.net.SocketTimeoutException -> getString(R.string.timeout_error)

                    is retrofit2.HttpException -> {
                        if (error.code() >= 500) {
                            getString(R.string.server_error)
                        } else {
                            getString(R.string.generic_error)
                        }
                    }

                    else -> getString(R.string.unknown_error)
                }

                layoutError.visibility = View.VISIBLE
            } else {
                layoutError.visibility = View.GONE
            }
        }

        buttonRetry.setOnClickListener {
            viewModel.loadMovieDetails(movieId)
        }

        viewModel.loadMovieDetails(movieId)
    }
}
