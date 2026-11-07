package ar.edu.unicen.seminario.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.unicen.seminario.model.MovieDetail
import ar.edu.unicen.seminario.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MovieDetailViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _movie = MutableLiveData<MovieDetail?>()
    val movie: LiveData<MovieDetail?> = _movie

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<Throwable?>()
    val error: LiveData<Throwable?> = _error

    private var loadJob: Job? = null

    fun loadMovieDetails(movieId: Int) {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _movie.value = null

            try {
                repository.getMovieDetails(movieId)
                    .onSuccess { detail ->
                        _movie.value = detail
                    }
                    .onFailure { throwable ->
                        _error.value = throwable
                    }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _error.value = exception
            } finally {
                _isLoading.value = false
            }
        }
    }
}
