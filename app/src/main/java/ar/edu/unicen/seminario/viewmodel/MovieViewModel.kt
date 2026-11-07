package ar.edu.unicen.seminario.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.unicen.seminario.model.Movie
import ar.edu.unicen.seminario.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MovieViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _movies = MutableLiveData<List<Movie>>(emptyList())
    val movies: LiveData<List<Movie>> = _movies

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isEmptySearch = MutableLiveData(false)
    private val _isPaginationError = MutableLiveData(false)
    val isPaginationError: LiveData<Boolean> = _isPaginationError
    val isEmptySearch: LiveData<Boolean> = _isEmptySearch

    private var currentPage = 0
    private var totalPages = Int.MAX_VALUE
    private var isLoadingMore = false
    private var isSearching = false
    private var requestVersion = 0

    private var currentJob: Job? = null

    fun loadPopularMovies() {
        requestVersion++
        currentJob?.cancel()
        currentJob = null

        isSearching = false
        isLoadingMore = false
        currentPage = 0
        totalPages = Int.MAX_VALUE

        _movies.value = emptyList()
        _isEmptySearch.value = false
        _isPaginationError.value = false

        loadPopularPage(1, append = false)
    }

    fun loadNextPopularPage() {
        if (isSearching || isLoadingMore) return
        if (currentPage >= totalPages) return

        loadPopularPage(currentPage + 1, append = true)
    }

    private fun loadPopularPage(page: Int, append: Boolean) {
        if (isLoadingMore) return

        isLoadingMore = true
        _isLoading.value = true
        _errorMessage.value = null
        _isPaginationError.value = false

        val version = requestVersion

        currentJob = viewModelScope.launch {
            try {
                repository.getPopularMovies(page = page)
                    .onSuccess { result ->
                        if (version != requestVersion) return@onSuccess

                        _movies.value = if (append) {
                            _movies.value.orEmpty() + result.movies
                        } else {
                            result.movies
                        }

                        currentPage = page
                        totalPages = result.totalPages
                    }
                    .onFailure { error ->
                        if (version != requestVersion) return@onFailure

                        _isPaginationError.value = append
                        _errorMessage.value = error.message
                    }
            } catch (error: CancellationException) {
                throw error
            } finally {
                if (version == requestVersion) {
                    isLoadingMore = false
                    _isLoading.value = false
                }
            }
        }
    }

    fun searchMovies(query: String) {
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            clearSearch()
            return
        }

        requestVersion++
        currentJob?.cancel()
        isLoadingMore = false
        isSearching = true
        _isPaginationError.value = false

        val version = requestVersion

        _isLoading.value = true
        _errorMessage.value = null
        _isEmptySearch.value = false

        currentJob = viewModelScope.launch {
            try {
                repository.searchMovies(cleanQuery)
                    .onSuccess { result ->
                        if (version != requestVersion) return@onSuccess

                        _movies.value = result
                        _isEmptySearch.value = result.isEmpty()
                    }
                    .onFailure { error ->
                        if (version != requestVersion) return@onFailure
                        _errorMessage.value = error.message
                    }
            } catch (error: CancellationException) {
                throw error
            } finally {
                if (version == requestVersion) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun clearSearch() {
        loadPopularMovies()
    }
}
