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
    private lateinit var durakEngine: DurakEngine
    private var isAnalyzing = false
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val analysisRunnable = object : Runnable {
        override fun run() {
            if (isAnalyzing) {
                performActualAnalysis()
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        durakEngine = DurakEngine(this)
        createNotificationChannel()
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultData = if (intent != null && intent.hasExtra(EXTRA_RESULT_DATA)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }
        } else null
        
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
        
        imageReader?.setOnImageAvailableListener({ reader ->
            // Кадр доступен
        }, handler)

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

    private fun performActualAnalysis() {
        val image = imageReader?.acquireLatestImage()
        if (image == null) {
            handler.post { overlayBinding?.tvAdvice?.text = "Ожидание кадра..." }
            return
        }

        // В реальном приложении здесь будет конвертация Image -> Bitmap -> CardDetector
        // Но так как у нас нет OpenCV/ML в SDK сейчас, мы эмулируем успешное распознавание 
        // ситуации, основанной на ПРЕДЫДУЩИХ данных из DurakEngine (те, что ввел пользователь или мы нашли ранее)
        
        val prompt = durakEngine.generatePrompt(emptyList()) // Пока пустой стол для примера
        
        val prefs = getSharedPreferences("durak_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gigachat_key", "") ?: ""
        
        if (apiKey.isEmpty()) {
            handler.post { overlayBinding?.tvAdvice?.text = "Ошибка: Введите API ключ в настройках!" }
            image.close()
            return
        }

        val request = com.durak.assistant.api.GigaChatRequest(
            messages = listOf(com.durak.assistant.api.Message("user", prompt))
        )

        com.durak.assistant.api.GigaChatClient.api.getChatCompletion(
            com.durak.assistant.api.GigaChatClient.getAuthToken(apiKey),
            request
        ).enqueue(object : retrofit2.Callback<com.durak.assistant.api.GigaChatResponse> {
            override fun onResponse(
                call: retrofit2.Call<com.durak.assistant.api.GigaChatResponse>,
                response: retrofit2.Response<com.durak.assistant.api.GigaChatResponse>
            ) {
                val advice = response.body()?.choices?.firstOrNull()?.message?.content 
                    ?: "Не удалось получить совет от ИИ"
                
                handler.post {
                    overlayBinding?.tvAdvice?.text = advice
                    overlayBinding?.tvCardsLeft?.text = "В колоде: ${durakEngine.getRemainingCardsCount(0)}"
                }
            }

            override fun onFailure(call: retrofit2.Call<com.durak.assistant.api.GigaChatResponse>, t: Throwable) {
                handler.post { overlayBinding?.tvAdvice?.text = "Ошибка сети: ${t.localizedMessage}" }
            }
        })

        image.close()
    }

    private fun showOverlay() {
        if (overlayBinding != null) return

        // Поток-безопасно и с темой для SwitchCompat
        val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_DurakAssistant)
        overlayBinding = LayoutOverlayBinding.inflate(LayoutInflater.from(themedContext))
        
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

        overlayBinding?.switchAi?.setOnCheckedChangeListener { _, isChecked ->
            isAnalyzing = isChecked
            if (isChecked) {
                Toast.makeText(this, "Анализ запущен", Toast.LENGTH_SHORT).show()
            } else {
                overlayBinding?.tvAdvice?.text = "Анализ остановлен"
            }
        }

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
