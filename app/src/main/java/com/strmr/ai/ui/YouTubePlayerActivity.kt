package com.strmr.ai.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.strmr.ai.R
import com.strmr.ai.databinding.ActivityYoutubePlayerBinding

class YouTubePlayerActivity : ComponentActivity() {
    
    private lateinit var binding: ActivityYoutubePlayerBinding
    
    companion object {
        const val EXTRA_YOUTUBE_KEY = "youtube_key"
        const val EXTRA_MOVIE_TITLE = "movie_title"
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val youtubeKey = intent.getStringExtra(EXTRA_YOUTUBE_KEY)
        val movieTitle = intent.getStringExtra(EXTRA_MOVIE_TITLE)
        
        if (youtubeKey == null) {
            finish()
            return
        }
        
        // Handle back press with the modern callback approach
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        
        setupWebView(youtubeKey, movieTitle)
    }
    
    private fun setupWebView(youtubeKey: String, movieTitle: String?) {
        binding.webView.apply {
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            
            // Load YouTube embed URL
            val embedUrl = "https://www.youtube.com/embed/$youtubeKey?autoplay=1&fs=1&rel=0"
            
            // Create HTML with YouTube embed
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            background-color: #000;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                        }
                        iframe {
                            width: 100vw;
                            height: 100vh;
                            border: none;
                        }
                        .title {
                            position: absolute;
                            top: 20px;
                            left: 20px;
                            color: white;
                            font-family: Arial, sans-serif;
                            font-size: 18px;
                            z-index: 1000;
                            background: rgba(0,0,0,0.7);
                            padding: 10px;
                            border-radius: 5px;
                        }
                    </style>
                </head>
                <body>
                    ${if (movieTitle != null) "<div class=\"title\">$movieTitle - Trailer</div>" else ""}
                    <iframe 
                        src="$embedUrl" 
                        frameborder="0" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            
            loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        }
        
        // Handle back button
        binding.closeButton.setOnClickListener {
            finish()
        }
    }
    
    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}