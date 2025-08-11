package com.strmr.ai.util

/**
 * Maps TMDB genre IDs to genre names
 * Based on TMDB's official genre list for movies and TV shows
 */
object TmdbGenreMapper {
    
    private val movieGenres = mapOf(
        28 to "Action",
        12 to "Adventure", 
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        14 to "Fantasy",
        36 to "History",
        27 to "Horror",
        10402 to "Music",
        9648 to "Mystery",
        10749 to "Romance",
        878 to "Science Fiction",
        10770 to "TV Movie",
        53 to "Thriller",
        10752 to "War",
        37 to "Western"
    )
    
    private val tvGenres = mapOf(
        10759 to "Action & Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        10762 to "Kids",
        9648 to "Mystery",
        10763 to "News",
        10764 to "Reality",
        10765 to "Sci-Fi & Fantasy",
        10766 to "Soap",
        10767 to "Talk",
        10768 to "War & Politics",
        37 to "Western"
    )
    
    /**
     * Maps movie genre IDs to genre names
     */
    fun getMovieGenres(genreIds: List<Int>): List<String> {
        return genreIds.mapNotNull { movieGenres[it] }
    }
    
    /**
     * Maps TV show genre IDs to genre names
     */
    fun getTvGenres(genreIds: List<Int>): List<String> {
        return genreIds.mapNotNull { tvGenres[it] }
    }
}