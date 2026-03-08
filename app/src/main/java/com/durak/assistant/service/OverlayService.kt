package com.durak.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.durak.assistant.R
import com.durak.assistant.databinding.LayoutOverlayBinding
import com.durak.assistant.logic.DurakEngine

class OverlayService : Service() {

    companion object {
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val CHANNEL_ID = "OverlayServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var overlayBinding: LayoutOverlayBinding? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val analysisRunnable = object : Runnable {
        override fun run() {
            updateOverlayWithMockAdvice()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ассистент Дурака запущен")
            .setContentText("Ищите матовое окно поверх игры")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (resultData != null) {
            try {
                setupProjection(resultData)
                showOverlay()
                startAnalysisLoop()
                Toast.makeText(this, "Ассистент работает!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        return START_NOT_STICKY
    }

    private fun setupProjection(data: Intent) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(Activity.RESULT_OK, data)

        // Android 14+ требует регистрации Callback ДО создания VirtualDisplay
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopSelf()
            }
        }, handler)

        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "DurakCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun startAnalysisLoop() {
        handler.post(analysisRunnable)
    }

    private fun updateOverlayWithMockAdvice() {
        val advices = listOf(
            "Совет: Жду начала раздачи...",
            "Совет: Сбрасывай мелкие карты.",
            "Совет: Противник зашел с 8ки, бей 10кой.",
            "Совет: В колоде осталось 12 карт.",
            "Анализ: Ищу карты на столе..."
        )
        val randomAdvice = advices.random()
        
        handler.post {
            overlayBinding?.tvAdvice?.text = randomAdvice
            overlayBinding?.tvCardsLeft?.text = "В колоде: ~${(10..36).random()}"
        }
    }

    private fun showOverlay() {
        if (overlayBinding != null) return

        overlayBinding = LayoutOverlayBinding.inflate(LayoutInflater.from(this))
        
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        windowManager.addView(overlayBinding?.root, params)

        val durakEngine = DurakEngine(this)

        // Make it draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayBinding?.root?.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayBinding?.root, params)
                    true
                }
                else -> false
            }
        }

        overlayBinding?.btnMarkBito?.setOnClickListener {
            // В реальной жизни мы бы брали текущие карты со стола. 
            // Сейчас просто эмулируем уход нескольких карт в биту для демонстрации памяти.
            durakEngine.updateGameState(emptyList(), emptyList(), "Черви", true)
            Toast.makeText(this, "Карты ушли в биту. ИИ запомнил.", Toast.LENGTH_SHORT).show()
        }
        
        overlayBinding?.btnSettings?.setOnClickListener {
            val intent = Intent(this, com.durak.assistant.SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Durak Assistant",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(analysisRunnable)
        overlayBinding?.let { 
            try { windowManager.removeView(it.root) } catch (e: Exception) {}
        }
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
    }
}
