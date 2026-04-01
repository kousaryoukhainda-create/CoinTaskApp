package com.cointask.user.player

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLDecoder

/**
 * Sealed class representing different video providers
 */
sealed class VideoProvider {
    abstract val name: String
    abstract fun extractVideoId(url: String): String?
    abstract fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String

    data object YouTube : VideoProvider() {
        override val name = "YouTube"

        override fun extractVideoId(url: String): String? {
            try {
                val decodedUrl = URLDecoder.decode(url, "UTF-8")
                
                val patterns = listOf(
                    Regex("[?&]v=([a-zA-Z0-9_-]{11})"),           // https://youtube.com/watch?v=VIDEO_ID
                    Regex("[?&]v=([a-zA-Z0-9_-]+)"),              // https://youtube.com/watch?v=VIDEO_ID (fallback)
                    Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),      // https://youtu.be/VIDEO_ID
                    Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),         // https://youtu.be/VIDEO_ID (fallback)
                    Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"), // https://youtube.com/embed/VIDEO_ID
                    Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)"),    // https://youtube.com/embed/VIDEO_ID (fallback)
                    Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})"),     // https://youtube.com/v/VIDEO_ID
                    Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"), // https://youtube.com/shorts/VIDEO_ID
                    Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"), // https://youtube.com/shorts/VIDEO_ID (fallback)
                    Regex("youtube\\.com/live/([a-zA-Z0-9_-]+)"),   // https://youtube.com/live/VIDEO_ID
                    Regex("v=([a-zA-Z0-9_-]+)")                   // Fallback: any v= parameter
                )

                for (pattern in patterns) {
                    val match = pattern.find(decodedUrl)
                    if (match != null) {
                        val videoId = match.groupValues[1]
                        Log.d(TAG, "YouTube: Extracted ID '$videoId'")
                        return videoId
                    }
                }

                // Final fallback: extract last path segment
                val pathSegment = decodedUrl.substringAfterLast("/").substringBefore("?").substringBefore("&")
                if (pathSegment.isNotEmpty() && pathSegment.length >= 11) {
                    Log.w(TAG, "YouTube: Using fallback extraction: '$pathSegment'")
                    return pathSegment
                }
            } catch (e: Exception) {
                Log.e(TAG, "YouTube: Error extracting video ID: ${e.message}")
            }

            Log.e(TAG, "YouTube: Failed to extract ID from URL: $url")
            return null
        }

        override fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String {
            val autoplayParam = if (autoplay) "1" else "0"
            
            val trackingScript = if (enableTracking) """
                <script>
                    var videoStarted = false;
                    var videoError = false;
                    var checkCount = 0;

                    // Listen for global errors
                    window.onerror = function(msg, url, line, col, error) {
                        console.error('Global error: ' + msg);
                        if (!videoError && msg.toString().includes('embed') && msg.toString().includes('not found')) {
                            videoError = true;
                            VideoPlayerInterface.onVideoError('153', 'Video not found or invalid');
                        }
                        return false;
                    };

                    // YouTube IFrame API
                    var tag = document.createElement('script');
                    tag.src = 'https://www.youtube.com/iframe_api';
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    if (firstScriptTag && firstScriptTag.parentNode) {
                        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
                    }

                    function onYouTubeIframeAPIReady() {
                        console.log('YouTube IFrame API ready');
                        try {
                            var player = new YT.Player('video', {
                                playerVars: {
                                    'autoplay': $autoplayParam,
                                    'controls': 1,
                                    'rel': 0,
                                    'modestbranding': 1
                                },
                                events: {
                                    'onReady': function(event) {
                                        console.log('Player ready');
                                        if ($autoplayParam) {
                                            event.target.playVideo();
                                        }
                                    },
                                    'onStateChange': function(event) {
                                        console.log('State change: ' + event.data);
                                        if (event.data == YT.PlayerState.PLAYING && !videoStarted) {
                                            videoStarted = true;
                                            VideoPlayerInterface.onVideoStart();
                                        }
                                        if (event.data == YT.PlayerState.ENDED) {
                                            VideoPlayerInterface.onVideoEnded();
                                        }
                                        if (event.data == YT.PlayerState.UNSTARTED && !videoStarted) {
                                            checkCount++;
                                            if (checkCount > 5) {
                                                videoError = true;
                                                VideoPlayerInterface.onVideoError('unstarted', 'Video failed to start');
                                            }
                                        }
                                    },
                                    'onError': function(event) {
                                        console.error('Player error: ' + event.data);
                                        if (!videoError) {
                                            videoError = true;
                                            var messages = {
                                                2: 'Invalid video ID',
                                                5: 'HTML5 player error',
                                                100: 'Video not found or removed',
                                                101: 'Video cannot be played in embedded player',
                                                150: 'Video cannot be played in embedded player',
                                                152: 'Video owner restricts embedding'
                                            };
                                            VideoPlayerInterface.onVideoError(
                                                event.data.toString(),
                                                messages[event.data] || 'Playback error: ' + event.data
                                            );
                                        }
                                    }
                                }
                            });
                        } catch (e) {
                            console.error('Error creating player: ' + e);
                            if (!videoError) {
                                videoError = true;
                                VideoPlayerInterface.onVideoError('init', 'Failed to initialize player');
                            }
                        }
                    }

                    // Fallback timeout
                    setTimeout(function() {
                        if (!videoStarted && !videoError) {
                            var iframe = document.getElementById('video');
                            if (iframe) {
                                videoError = true;
                                VideoPlayerInterface.onVideoError('timeout', 'Video loading timeout');
                            }
                        }
                    }, 10000);
                </script>
            """ else ""

            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    #video { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <div id="player">
                    <iframe id="video" width="100%" height="100%"
                        src="https://www.youtube.com/embed/$videoId?autoplay=$autoplayParam&rel=0&controls=1&modestbranding=1&enablejsapi=1"
                        frameborder="0"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                        allowfullscreen>
                    </iframe>
                </div>
                $trackingScript
            </body>
            </html>
            """.trimIndent()
        }
    }

    data object Vimeo : VideoProvider() {
        override val name = "Vimeo"

        override fun extractVideoId(url: String): String? {
            try {
                val decodedUrl = URLDecoder.decode(url, "UTF-8")
                
                val patterns = listOf(
                    Regex("vimeo\\.com/(\\d+)"),                  // https://vimeo.com/VIDEO_ID
                    Regex("vimeo\\.com/channels/\\d+/(\\d+)"),    // https://vimeo.com/channels/.../VIDEO_ID
                    Regex("vimeo\\.com/groups/\\d+/videos/(\\d+)"), // https://vimeo.com/groups/.../videos/VIDEO_ID
                    Regex("player\\.vimeo\\.com/video/(\\d+)")    // https://player.vimeo.com/video/VIDEO_ID
                )

                for (pattern in patterns) {
                    val match = pattern.find(decodedUrl)
                    if (match != null) {
                        val videoId = match.groupValues[1]
                        Log.d(TAG, "Vimeo: Extracted ID '$videoId'")
                        return videoId
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vimeo: Error extracting video ID: ${e.message}")
            }

            Log.e(TAG, "Vimeo: Failed to extract ID from URL: $url")
            return null
        }

        override fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String {
            val autoplayAttr = if (autoplay) "1" else "0"
            
            val trackingScript = if (enableTracking) """
                <script src="https://player.vimeo.com/api/player.js"></script>
                <script>
                    var iframe = document.querySelector('iframe');
                    var player = new Vimeo.Player(iframe);

                    player.ready().then(function() {
                        console.log('Vimeo player ready');
                        if ($autoplayAttr === "1") {
                            player.play();
                        }
                    });

                    player.on('play', function() {
                        console.log('Vimeo playing');
                        VideoPlayerInterface.onVideoStart();
                    });

                    player.on('ended', function() {
                        console.log('Vimeo ended');
                        VideoPlayerInterface.onVideoEnded();
                    });

                    player.on('error', function(error) {
                        console.error('Vimeo error: ' + error);
                        VideoPlayerInterface.onVideoError(error.code.toString(), error.message || 'Vimeo playback error');
                    });
                </script>
            """ else ""

            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe src="https://player.vimeo.com/video/$videoId?autoplay=$autoplayAttr&title=0&byline=0&portrait=0"
                    frameborder="0"
                    allow="autoplay; fullscreen; picture-in-picture"
                    allowfullscreen>
                </iframe>
                $trackingScript
            </body>
            </html>
            """.trimIndent()
        }
    }

    data object Dailymotion : VideoProvider() {
        override val name = "Dailymotion"

        override fun extractVideoId(url: String): String? {
            try {
                val decodedUrl = URLDecoder.decode(url, "UTF-8")
                
                val patterns = listOf(
                    Regex("dailymotion\\.com/video/([a-zA-Z0-9]+)"),  // https://dailymotion.com/video/ID
                    Regex("dai\\.ly/([a-zA-Z0-9]+)")                  // https://dai.ly/ID
                )

                for (pattern in patterns) {
                    val match = pattern.find(decodedUrl)
                    if (match != null) {
                        val videoId = match.groupValues[1]
                        Log.d(TAG, "Dailymotion: Extracted ID '$videoId'")
                        return videoId
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dailymotion: Error extracting video ID: ${e.message}")
            }

            Log.e(TAG, "Dailymotion: Failed to extract ID from URL: $url")
            return null
        }

        override fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String {
            val autoplayAttr = if (autoplay) "1" else "0"
            
            val trackingScript = if (enableTracking) """
                <script>
                    var iframe = document.querySelector('iframe');
                    var player;
                    var videoStarted = false;
                    var videoError = false;

                    // Dailymotion Player API
                    iframe.addEventListener('load', function() {
                        console.log('Dailymotion iframe loaded');
                        // Send commands via postMessage
                        iframe.contentWindow.postMessage(JSON.stringify({
                            event: 'command',
                            command: 'play'
                        }), '*');
                    });

                    // Listen for player messages
                    window.addEventListener('message', function(e) {
                        try {
                            var data = JSON.parse(e.data);
                            if (data.event === 'play' && !videoStarted) {
                                videoStarted = true;
                                VideoPlayerInterface.onVideoStart();
                            }
                            if (data.event === 'end') {
                                VideoPlayerInterface.onVideoEnded();
                            }
                            if (data.event === 'error') {
                                if (!videoError) {
                                    videoError = true;
                                    VideoPlayerInterface.onVideoError('error', 'Dailymotion player error');
                                }
                            }
                        } catch (err) {
                            // Ignore parse errors
                        }
                    });

                    // Fallback timeout
                    setTimeout(function() {
                        if (!videoStarted && !videoError) {
                            videoError = true;
                            VideoPlayerInterface.onVideoError('timeout', 'Video loading timeout');
                        }
                    }, 10000);
                </script>
            """ else ""

            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe src="https://www.dailymotion.com/embed/video/$videoId?autoplay=$autoplayAttr&queue=false&syndication=207619"
                    frameborder="0"
                    allow="autoplay; fullscreen; picture-in-picture"
                    allowfullscreen>
                </iframe>
                $trackingScript
            </body>
            </html>
            """.trimIndent()
        }
    }

    data object Direct : VideoProvider() {
        override val name = "Direct"

        override fun extractVideoId(url: String): String? {
            // For direct videos, the URL itself is the video source
            return url
        }

        override fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String {
            val autoplayAttr = if (autoplay) "autoplay" else ""

            val trackingScript = if (enableTracking) """
                <script>
                    var video = document.querySelector('#video');
                    var videoError = false;
                    var loadTimeout;

                    video.addEventListener('play', function() {
                        console.log('Direct video playing');
                        VideoPlayerInterface.onVideoStart();
                    });

                    video.addEventListener('ended', function() {
                        console.log('Direct video ended');
                        VideoPlayerInterface.onVideoEnded();
                    });

                    video.addEventListener('error', function() {
                        if (!videoError) {
                            videoError = true;
                            var error = video.error;
                            var errorMsg = 'Unknown video error';
                            if (error) {
                                var messages = {
                                    1: 'Video loading aborted',
                                    2: 'Network error',
                                    3: 'Video decoding failed',
                                    4: 'Video format not supported'
                                };
                                errorMsg = messages[error.code] || 'Error code: ' + error.code;
                            }
                            console.error('Video error: ' + errorMsg);
                            VideoPlayerInterface.onVideoError(error ? error.code.toString() : '0', errorMsg);
                        }
                    });

                    video.addEventListener('loadedmetadata', function() {
                        console.log('Video metadata loaded');
                        if ($autoplayAttr && video.paused) {
                            video.play().catch(function(e) {
                                console.error('Autoplay failed: ' + e);
                            });
                        }
                    });

                    video.addEventListener('loadstart', function() {
                        console.log('Video load started');
                        clearTimeout(loadTimeout);
                        loadTimeout = setTimeout(function() {
                            if (!videoError && video.readyState < 3) {
                                videoError = true;
                                VideoPlayerInterface.onVideoError('timeout', 'Video loading timeout');
                            }
                        }, 15000);
                    });

                    video.addEventListener('canplay', function() {
                        console.log('Video can play');
                        clearTimeout(loadTimeout);
                    });

                    // Fallback timeout
                    setTimeout(function() {
                        if (video.paused && !video.ended && !videoError && video.readyState < 3) {
                            videoError = true;
                            VideoPlayerInterface.onVideoError('timeout', 'Video failed to load');
                        }
                    }, 20000);
                </script>
            """ else ""

            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    video { width: 100%; height: 100%; object-fit: contain; }
                </style>
            </head>
            <body>
                <video id="video" width="100%" height="100%" controls $autoplayAttr playsinline crossorigin="anonymous">
                    <source src="$videoId" type="video/mp4">
                    <source src="$videoId" type="video/webm">
                    <source src="$videoId" type="video/ogg">
                    Your browser does not support the video tag.
                </video>
                $trackingScript
            </body>
            </html>
            """.trimIndent()
        }
    }

    companion object {
        private const val TAG = "VideoProvider"

        /**
         * Detects the video provider from a URL
         */
        fun fromUrl(url: String): VideoProvider {
            val lowerUrl = url.lowercase()
            return when {
                lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> YouTube
                lowerUrl.contains("vimeo.com") -> Vimeo
                lowerUrl.contains("dailymotion.com") || lowerUrl.contains("dai.ly") -> Dailymotion
                else -> Direct
            }
        }

        /**
         * Gets the external app URL for opening videos externally
         */
        fun getExternalUrl(url: String): String {
            val provider = fromUrl(url)
            return when (provider) {
                is YouTube -> {
                    val videoId = YouTube.extractVideoId(url)
                    if (videoId != null) "vnd.youtube:$videoId" else url
                }
                is Vimeo -> url
                is Dailymotion -> url
                is Direct -> url
            }
        }
    }
}

/**
 * Interface for tracking video playback events
 */
interface VideoPlaybackListener {
    fun onVideoStarted()
    fun onVideoEnded()
    fun onVideoError(errorCode: String, message: String)
}

/**
 * Helper class for configuring and loading videos in a WebView
 */
class VideoPlayerConfig private constructor(
    val url: String,
    val provider: VideoProvider,
    val autoplay: Boolean,
    val enableTracking: Boolean,
    val heightDp: Int,
    val listener: VideoPlaybackListener?
) {
    class Builder(private val url: String) {
        private var provider: VideoProvider? = null
        private var autoplay: Boolean = true
        private var enableTracking: Boolean = true
        private var heightDp: Int = 300
        private var listener: VideoPlaybackListener? = null

        fun provider(provider: VideoProvider) = apply { this.provider = provider }
        fun autoPlay(autoplay: Boolean) = apply { this.autoplay = autoplay }
        fun enableTracking(enable: Boolean) = apply { this.enableTracking = enable }
        fun height(heightDp: Int) = apply { this.heightDp = heightDp }
        fun listener(listener: VideoPlaybackListener) = apply { this.listener = listener }

        fun build(): VideoPlayerConfig {
            val resolvedProvider = provider ?: VideoProvider.fromUrl(url)
            return VideoPlayerConfig(url, resolvedProvider, autoplay, enableTracking, heightDp, listener)
        }
    }
}

/**
 * Helper object to setup WebView for video playback
 */
object VideoPlayerHelper {

    private const val TAG = "VideoPlayerHelper"

    // Track if this is the first load attempt for retry functionality
    private var currentConfig: VideoPlayerConfig? = null

    /**
     * Sets up a WebView to play a video with the given configuration
     */
    fun setupWebView(webView: WebView, config: VideoPlayerConfig) {
        currentConfig = config

        Log.d(TAG, "Setting up WebView for ${config.provider.name} video: ${config.url}")

        // Enable hardware acceleration for better video performance
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL
            setSupportZoom(false)
        }

        // Clear any existing data for fresh load
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()

        // Add JavaScript interface for tracking
        config.listener?.let { listener ->
            webView.removeJavascriptInterface("VideoPlayerInterface")
            webView.addJavascriptInterface(object {
                @JavascriptInterface
                fun onVideoStart() {
                    Log.d(TAG, "✓ Video started playing")
                    listener.onVideoStarted()
                }

                @JavascriptInterface
                fun onVideoError(errorCode: String, message: String) {
                    Log.e(TAG, "✗ Video error: $errorCode - $message")
                    listener.onVideoError(errorCode, message)
                }

                @JavascriptInterface
                fun onVideoEnded() {
                    Log.d(TAG, "✓ Video ended")
                    listener.onVideoEnded()
                }
            }, "VideoPlayerInterface")
        }

        // Set WebChromeClient for console logging and fullscreen support
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                Log.d(TAG, "Console: ${consoleMessage.message()}")
                return true
            }

            override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
                Log.d(TAG, "Video entered fullscreen")
                super.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {
                Log.d(TAG, "Video exited fullscreen")
                super.onHideCustomView()
            }
        }

        // Set WebViewClient for error handling
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished: $url")
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                Log.e(TAG, "WebView error: $description (code $errorCode)")
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                val statusCode = errorResponse?.statusCode ?: 0
                if (statusCode >= 400) {
                    Log.e(TAG, "HTTP error: $statusCode for ${request?.url}")
                }
            }
        }

        // Extract video ID and generate HTML
        val provider = config.provider
        val videoId = provider.extractVideoId(config.url)

        val html = if (videoId.isNullOrEmpty()) {
            Log.e(TAG, "Failed to extract video ID from URL")
            generateErrorHtml("Could not extract video ID from URL. Please check the link.")
        } else {
            Log.d(TAG, "Using provider: ${provider.name}, Video ID: '$videoId'")
            provider.generateEmbedHtml(videoId, config.autoplay, config.enableTracking)
        }

        // Use appropriate base URL for each provider
        val baseUrl = when (provider) {
            is VideoProvider.YouTube -> "https://www.youtube.com/"
            is VideoProvider.Vimeo -> "https://player.vimeo.com/"
            is VideoProvider.Dailymotion -> "https://www.dailymotion.com/"
            is VideoProvider.Direct -> null // Use null for direct URLs to avoid rewriting
        }

        if (baseUrl != null) {
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        } else {
            webView.loadData(html, "text/html", "UTF-8")
        }
    }

    /**
     * Retry loading the video with the previous configuration
     */
    fun retry(webView: WebView) {
        currentConfig?.let { config ->
            Log.d(TAG, "Retrying video load")
            setupWebView(webView, config)
        } ?: run {
            Log.e(TAG, "No configuration available for retry")
        }
    }

    private fun generateErrorHtml(message: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { 
                    display: flex; 
                    align-items: center; 
                    justify-content: center; 
                    min-height: 100vh; 
                    background: #1a1a1a; 
                    color: #fff; 
                    font-family: sans-serif;
                    padding: 20px;
                }
                .error-container { text-align: center; }
                .error-icon { font-size: 48px; margin-bottom: 20px; }
                .error-message { font-size: 16px; line-height: 1.5; }
            </style>
        </head>
        <body>
            <div class="error-container">
                <div class="error-icon">⚠️</div>
                <div class="error-message">$message</div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Creates a new configuration builder for the given URL
     */
    fun configure(url: String): VideoPlayerConfig.Builder {
        return VideoPlayerConfig.Builder(url)
    }

    /**
     * Gets the external app URL for opening videos externally
     */
    fun getExternalUrl(url: String): String {
        return VideoProvider.getExternalUrl(url)
    }
}
