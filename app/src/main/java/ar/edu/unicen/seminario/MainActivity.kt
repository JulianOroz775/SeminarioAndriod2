
package ar.edu.unicen.seminario

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ar.edu.unicen.seminario.network.RetrofitInstance
import ar.edu.unicen.seminario.repository.MovieRepository
import ar.edu.unicen.seminario.ui.movies.MovieAdapter
import ar.edu.unicen.seminario.viewmodel.MovieViewModel
import ar.edu.unicen.seminario.viewmodel.MovieViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MovieViewModel
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var recyclerMovies: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutError: View
    private lateinit var textError: TextView
    private lateinit var textEmptySearch: TextView
    private lateinit var buttonRetry: Button
    private lateinit var editSearchMovie: EditText

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonOpenWishlist =
            findViewById<Button>(R.id.buttonOpenWishlist)

        buttonOpenWishlist.setOnClickListener {
            startActivity(Intent(this, WishlistActivity::class.java))
        }

        recyclerMovies = findViewById(R.id.recyclerMovies)
        editSearchMovie = findViewById(R.id.editSearchMovie)
        progressLoading = findViewById(R.id.progressLoading)
        layoutError = findViewById(R.id.layoutError)
        textError = findViewById(R.id.textError)
        textEmptySearch = findViewById(R.id.textEmptySearch)
        buttonRetry = findViewById(R.id.buttonRetry)

        movieAdapter = MovieAdapter(
            onMovieClick = { movie ->
                val intent = Intent(this, MovieDetailActivity::class.java)
                intent.putExtra("movie_id", movie.id)
                startActivity(intent)
            }
        )

        val layoutManager = LinearLayoutManager(this)
        recyclerMovies.layoutManager = layoutManager
        recyclerMovies.adapter = movieAdapter

        recyclerMovies.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy > 0) {
                        val totalItems = layoutManager.itemCount
                        val lastVisibleItem =
                            layoutManager.findLastVisibleItemPosition()

                        if (totalItems > 0 &&
                            lastVisibleItem >= totalItems - 3
                        ) {
                            viewModel.loadNextPopularPage()
                        }
                    }
                }
            }
        )

        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN

        if (token.isBlank()) {
            showError(getString(R.string.missing_token_error))
            return
        }

        val repository = MovieRepository(
            RetrofitInstance.movieApi,
            token
        )

        val factory = MovieViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)
            .get(MovieViewModel::class.java)

        observeViewModel()
        setupSearch()

        buttonRetry.setOnClickListener {
            val query = editSearchMovie.text.toString().trim()

            if (viewModel.isPaginationError.value == true) {
                viewModel.loadNextPopularPage()
            } else if (query.isBlank()) {
                viewModel.loadPopularMovies()
            } else {
                viewModel.searchMovies(query)
            }
        }

        viewModel.loadPopularMovies()
    }

    private fun setupSearch() {
        editSearchMovie.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                searchRunnable?.let {
                    searchHandler.removeCallbacks(it)
                }

                val query = s?.toString().orEmpty().trim()

                searchRunnable = Runnable {
                    if (query.isBlank()) {
                        viewModel.clearSearch()
                    } else {
                        viewModel.searchMovies(query)
                    }
                }

                searchRunnable?.let {
                    searchHandler.postDelayed(it, 500)
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun observeViewModel() {
        viewModel.movies.observe(this) { movies ->
            movieAdapter.updateMovies(movies)
            recyclerMovies.visibility =
                if (movies.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressLoading.visibility =
                if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                layoutError.visibility = View.GONE
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                showError(
                    mapErrorMessage(message),
                    viewModel.isPaginationError.value == true
                )
            } else {
                layoutError.visibility = View.GONE
            }
        }

        viewModel.isEmptySearch.observe(this) { isEmpty ->
            textEmptySearch.visibility =
                if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun mapErrorMessage(message: String): String {
        return when {
            message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("Failed to connect", ignoreCase = true) ->
                getString(R.string.network_error)

            message.contains("timeout", ignoreCase = true) ->
                getString(R.string.timeout_error)

            message.contains("500") ||
                    message.contains("502") ||
                    message.contains("503") ||
                    message.contains("504") ->
                getString(R.string.server_error)

            else -> getString(R.string.unknown_error)
        }
    }

    private fun showError(message: String, keepMovies: Boolean = false) {
        textError.text = message
        layoutError.visibility = View.VISIBLE

        if (!keepMovies) {
            recyclerMovies.visibility = View.GONE
        }

        textEmptySearch.visibility = View.GONE
        progressLoading.visibility = View.GONE
    }

    override fun onDestroy() {
        searchRunnable?.let {
            searchHandler.removeCallbacks(it)
        }

        super.onDestroy()
    }
}
