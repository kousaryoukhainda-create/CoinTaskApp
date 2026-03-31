package com.cointask.user.player

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

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
            val patterns = listOf(
                Regex("[?&]v=([a-zA-Z0-9_-]{11})"),           // https://youtube.com/watch?v=VIDEO_ID
                Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),      // https://youtu.be/VIDEO_ID
                Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"), // https://youtube.com/embed/VIDEO_ID
                Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})"),     // https://youtube.com/v/VIDEO_ID
                Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"), // https://youtube.com/watch?v=VIDEO_ID
                Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"), // https://youtube.com/shorts/VIDEO_ID
                Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"), // https://youtube.com/shorts/VIDEO_ID (fallback)
                Regex("v=([a-zA-Z0-9_-]{11})"),               // Fallback: any v= parameter
                Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),         // Fallback: short URL (any length)
                Regex("embed/([a-zA-Z0-9_-]+)"),              // Fallback: embed URL
                Regex("shorts/([a-zA-Z0-9_-]+)")              // Fallback: shorts URL
            )

            for (pattern in patterns) {
                val match = pattern.find(url)
                if (match != null) {
                    val videoId = match.groupValues[1]
                    Log.d("VideoProvider", "YouTube: Matched ID '$videoId' with pattern")
                    return videoId
                }
            }

            // Fallback: extract path segment
            val pathSegment = url.substringAfterLast("/").substringBefore("?")
            if (pathSegment.isNotEmpty() && pathSegment.length in 11..15) {
                Log.w("VideoProvider", "YouTube: Using fallback extraction: '$pathSegment'")
                return pathSegment
            }

            Log.e("VideoProvider", "YouTube: Failed to extract ID from URL: $url")
            return null
        }
        
        override fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String {
            val autoplayParam = if (autoplay) "1" else "0"
            val trackingScript = if (enableTracking) """
                <script>
                    // Listen for YouTube player errors
                    window.onerror = function(msg, url, line, col, error) {
                        console.error('Error: ' + msg + ' at ' + url + ':' + line);
                        if (msg.toString().includes('153') || msg.toString().includes('not found')) {
                            VideoPlayerInterface.onVideoError('153', 'Video not found or invalid video ID');
                        }
                        return false;
                    };

                    // Track YouTube player state
                    var tag = document.createElement('script');
                    tag.src = 'https://www.youtube.com/iframe_api';
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    function onYouTubeIframeAPIReady() {
                        var player = new YT.Player('video', {
                            events: {
                                'onStateChange': function(event) {
                                    if (event.data == YT.PlayerState.PLAYING) {
                                        VideoPlayerInterface.onVideoStart();
                                    }
                                    if (event.data == YT.PlayerState.ENDED) {
                                        VideoPlayerInterface.onVideoEnded();
                                    }
                                },
                                'onError': function(event) {
                                    var errorMsg = 'Player error: ' + event.data;
                                    if (event.data == 100) errorMsg = 'Video not found';
                                    if (event.data == 101 || event.data == 150) errorMsg = 'Video cannot be played';
                                    if (event.data == 153) errorMsg = 'Invalid video ID';
                                    VideoPlayerInterface.onVideoError(event.data.toString(), errorMsg);
                                }
                            }
                        });
                    }
                </script>
            """ else ""
            
            return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#000;">
            <iframe id="video" width="100%" height="100%"
                src="https://www.youtube.com/embed/$videoId?autoplay=$autoplayParam&rel=0&enablejsapi=1"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
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

                    video.addEventListener('play', function() {
                        VideoPlayerInterface.onVideoStart();
                    });

                    video.addEventListener('ended', function() {
                        VideoPlayerInterface.onVideoEnded();
                    });

                    video.addEventListener('error', function() {
                        var error = video.error;
                        var errorMsg = 'Unknown video error';
                        if (error) {
                            if (error.code == 1) errorMsg = 'Video loading aborted';
                            if (error.code == 2) errorMsg = 'Network error while loading video';
                            if (error.code == 3) errorMsg = 'Video decoding failed';
                            if (error.code == 4) errorMsg = 'Video format not supported';
                        }
                        VideoPlayerInterface.onVideoError(error ? error.code.toString() : '0', errorMsg);
                    });

                    video.addEventListener('loadedmetadata', function() {
                        console.log('Video metadata loaded successfully');
                    });

                    video.addEventListener('loadstart', function() {
                        console.log('Video load started');
                    });
                </script>
            """ else ""
            
            return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#000;">
            <video id="video" width="100%" height="100%" controls $autoplayAttr>
                <source src="$videoId">
                Your browser does not support the video tag.
            </video>
            $trackingScript
            </body>
            </html>
            """.trimIndent()
        }
    }
    
    data object Vimeo : VideoProvider() {
        override val name = "Vimeo"
        
        override fun extractVideoId(url: String): String? {
            val patterns = listOf(
                Regex("vimeo\\.com/(\\d+)"),                  // https://vimeo.com/VIDEO_ID
                Regex("vimeo\\.com/channels/\\d+/(\\d+)"),    // https://vimeo.com/channels/.../VIDEO_ID
                Regex("vimeo\\.com/groups/\\d+/videos/(\\d+)"), // https://vimeo.com/groups/.../videos/VIDEO_ID
                Regex("player\\.vimeo\\.com/video/(\\d+)")    // https://player.vimeo.com/video/VIDEO_ID
            )
            
            for (pattern in patterns) {
                val match = pattern.find(url)
                if (match != null) {
                    val videoId = match.groupValues[1]
                    Log.d("VideoProvider", "Vimeo: Matched ID '$videoId' with pattern")
                    return videoId
                }
            }
            
            Log.e("VideoProvider", "Vimeo: Failed to extract ID from URL: $url")
            return null
        }
        
        override fun generateEmbedHtml(videoId: String, autoplay: Boolean, enableTracking: Boolean): String {
            val autoplayAttr = if (autoplay) "true" else "false"
            val trackingScript = if (enableTracking) """
                <script src="https://player.vimeo.com/api/player.js"></script>
                <script>
                    var iframe = document.querySelector('iframe');
                    var player = new Vimeo.Player(iframe);
                    
                    player.on('play', function() {
                        VideoPlayerInterface.onVideoStart();
                    });
                    
                    player.on('ended', function() {
                        VideoPlayerInterface.onVideoEnded();
                    });
                    
                    player.on('error', function(error) {
                        VideoPlayerInterface.onVideoError(error.code.toString(), error.message);
                    });
                </script>
            """ else ""
            
            return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#000;">
            <iframe src="https://player.vimeo.com/video/$videoId?autoplay=$autoplayAttr"
                width="100%" height="100%"
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
    
    companion object {
        /**
         * Detects the video provider from a URL
         */
        fun fromUrl(url: String): VideoProvider {
            return when {
                url.contains("youtube.com") || url.contains("youtu.be") -> YouTube
                url.contains("vimeo.com") -> Vimeo
                else -> Direct
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
    
    /**
     * Sets up a WebView to play a video with the given configuration
     */
    fun setupWebView(webView: WebView, config: VideoPlayerConfig) {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        
        // Add JavaScript interface for tracking
        config.listener?.let { listener ->
            webView.addJavascriptInterface(object {
                @JavascriptInterface
                fun onVideoStart() {
                    Log.d(TAG, "Video started playing")
                    listener.onVideoStarted()
                }
                
                @JavascriptInterface
                fun onVideoError(errorCode: String, message: String) {
                    Log.e(TAG, "Video error: $errorCode - $message")
                    listener.onVideoError(errorCode, message)
                }
                
                @JavascriptInterface
                fun onVideoEnded() {
                    Log.d(TAG, "Video ended")
                    listener.onVideoEnded()
                }
            }, "VideoPlayerInterface")
        }
        
        // Set WebChromeClient for console logging
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                Log.d(TAG, "Console: ${consoleMessage.message()} " +
                    "(line ${consoleMessage.lineNumber()}, source ${consoleMessage.sourceId()})")
                return true
            }
        }
        
        // Set WebViewClient for error handling
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                Log.e(TAG, "WebView error: $description (code $errorCode)")
                if (errorCode >= 400) {
                    config.listener?.onVideoError(
                        errorCode.toString(),
                        "Failed to load video resource"
                    )
                }
            }
        }
        
        // Extract video ID and generate HTML
        val provider = config.provider
        val videoId = provider.extractVideoId(config.url)
        
        val html = if (videoId.isNullOrEmpty()) {
            Log.e(TAG, "Failed to extract video ID from URL: ${config.url}")
            generateErrorHtml("Could not extract video ID from URL")
        } else {
            Log.d(TAG, "Using provider: ${provider.name}, Video ID: '$videoId'")
            provider.generateEmbedHtml(videoId, config.autoplay, config.enableTracking)
        }
        
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
    
    private fun generateErrorHtml(message: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#1a1a1a;color:#fff;font-family:sans-serif;">
            <div style="text-align:center;">
                <p style="font-size:48px;margin-bottom:20px;">⚠️</p>
                <p style="font-size:18px;">$message</p>
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
}
