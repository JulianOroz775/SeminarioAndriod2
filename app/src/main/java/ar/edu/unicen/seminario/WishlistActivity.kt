package ar.edu.unicen.seminario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ar.edu.unicen.seminario.ui.movies.MovieAdapter

class WishlistActivity : AppCompatActivity() {

    private lateinit var wishlistManager: WishlistManager
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var recyclerWishlist: RecyclerView
    private lateinit var textWishlistEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        wishlistManager = WishlistManager(this)

        recyclerWishlist = findViewById(R.id.recyclerWishlist)
        textWishlistEmpty = findViewById(R.id.textWishlistEmpty)

        recyclerWishlist.layoutManager = LinearLayoutManager(this)

        movieAdapter = MovieAdapter(
            onMovieClick = { movie ->
                val intent = Intent(this, MovieDetailActivity::class.java)
                intent.putExtra("movie_id", movie.id)
                startActivity(intent)
            },
            showRemoveButton = true,
            onRemoveMovie = { movie ->
                wishlistManager.toggleFavorite(movie)
                movieAdapter.removeMovie(movie)
                updateEmptyState()
            }
        )

        recyclerWishlist.adapter = movieAdapter
    }

    override fun onResume() {
        super.onResume()
        loadWishlist()
    }

    private fun loadWishlist() {
        val movies = wishlistManager.getSavedMovies()

        movieAdapter.updateMovies(movies)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = movieAdapter.itemCount == 0

        recyclerWishlist.visibility =
            if (isEmpty) View.GONE else View.VISIBLE

        textWishlistEmpty.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }
}
