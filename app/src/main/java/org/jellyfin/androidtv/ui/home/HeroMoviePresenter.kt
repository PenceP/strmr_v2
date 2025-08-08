package org.jellyfin.androidtv.ui.home

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.api.service.TmdbApiService
import org.jellyfin.androidtv.ui.AsyncImageView

class HeroMoviePresenter : Presenter() {
    
    class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        val backdropImage: AsyncImageView = view.findViewById(R.id.hero_backdrop)
        val titleText: TextView = view.findViewById(R.id.hero_title)
        val subtitleText: TextView = view.findViewById(R.id.hero_subtitle)
        val overviewText: TextView = view.findViewById(R.id.hero_overview)
        val ratingText: TextView = view.findViewById(R.id.hero_rating)
        val yearText: TextView = view.findViewById(R.id.hero_year)
        val gradientOverlay: View = view.findViewById(R.id.hero_gradient_overlay)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_hero_movie, parent, false)
        
        // Set up gradient overlay
        val gradientOverlay = view.findViewById<View>(R.id.hero_gradient_overlay)
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(180, 0, 0, 0),
                Color.argb(220, 0, 0, 0)
            )
        )
        gradientOverlay.background = gradientDrawable
        
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        if (item !is HeroMovieItem) return
        
        val holder = viewHolder as ViewHolder
        val movie = item.movie
        val context = holder.view.context
        
        // Set movie details
        holder.titleText.text = movie.title
        holder.overviewText.text = movie.overview ?: "No description available"
        
        // Set rating
        if (movie.voteAverage > 0.0) {
            holder.ratingText.text = "★ %.1f".format(movie.voteAverage)
            holder.ratingText.visibility = View.VISIBLE
        } else {
            holder.ratingText.visibility = View.GONE
        }
        
        // Set year
        if (!movie.releaseDate.isNullOrEmpty()) {
            try {
                val year = movie.releaseDate.substring(0, 4)
                holder.yearText.text = year
                holder.yearText.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.yearText.visibility = View.GONE
            }
        } else {
            holder.yearText.visibility = View.GONE
        }
        
        // Set subtitle (year + rating)
        val subtitleParts = mutableListOf<String>()
        if (!movie.releaseDate.isNullOrEmpty()) {
            try {
                subtitleParts.add(movie.releaseDate.substring(0, 4))
            } catch (e: Exception) { }
        }
        if (movie.voteAverage > 0.0) {
            subtitleParts.add("★ %.1f".format(movie.voteAverage))
        }
        holder.subtitleText.text = subtitleParts.joinToString(" • ")
        
        // Load backdrop image
        val backdropUrl = movie.backdropPath?.let { path ->
            "${TmdbApiService.IMAGE_BASE_URL}${TmdbApiService.BACKDROP_SIZE_W1280}$path"
        }
        
        if (backdropUrl != null) {
            holder.backdropImage.load(
                url = backdropUrl,
                placeholder = ContextCompat.getDrawable(context, R.drawable.tile_land_tv)
            )
        } else {
            // Use default placeholder if no backdrop
            holder.backdropImage.setImageResource(R.drawable.tile_land_tv)
        }
    }
    
    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.backdropImage.setImageDrawable(null)
    }
}