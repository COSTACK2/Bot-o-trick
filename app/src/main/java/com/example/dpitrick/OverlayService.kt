package com.example.dpitrick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var dpiOriginal: Int = 0
    private var trickAtivo = false

    companion object {
        private const val CHANNEL_ID = "dpi_trick_channel"
        private const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        criarCanalNotificacao()
        startForeground(NOTIF_ID, criarNotificacao())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dpiOriginal = DpiUtils.getCurrentDensity(this)
        trickAtivo = PrefsManager.isTrickAtivo(this)

        if (PrefsManager.getDpiNormal(this, -1) == -1) {
            PrefsManager.setDpiNormal(this, dpiOriginal)
        }

        criarOverlay()
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DPI Trick",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Mantém a barra flutuante ativa"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun criarNotificacao(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Barra flutuante ativa")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun criarOverlay() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bar, null)
        overlayView = view

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(view, params)

        configurarBarraCompleta(view)
        configurarBotaoMinimizado(view)
        carregarValoresSalvos(view)
        habilitarFocoParaCampos(view)
    }

    private fun configurarBarraCompleta(view: View) {
        val fullBar = view.findViewById<View>(R.id.fullBar)
        val minimizedButton = view.findViewById<View>(R.id.minimizedButton)
        val inputDpiAlto = view.findViewById<EditText>(R.id.inputDpiAlto)
        val inputDpiNormal = view.findViewById<EditText>(R.id.inputDpiNormal)
        val btnTrick = view.findViewById<Button>(R.id.btnTrick)
        val btnMinimizar = view.findViewById<View>(R.id.btnMinimizar)
        val btnFechar = view.findViewById<View>(R.id.btnFechar)

        atualizarTextoBotaoTrick(btnTrick)

        btnTrick.setOnClickListener {
            if (!trickAtivo) {
                val dpiAlto = inputDpiAlto.text.toString().toIntOrNull()
                if (dpiAlto == null) {
                    inputDpiAlto.error = "Informe um DPI"
                    return@setOnClickListener
                }
                val dpiNormal = inputDpiNormal.text.toString().toIntOrNull()
                    ?: PrefsManager.getDpiNormal(this, dpiOriginal)

                PrefsManager.setDpiAlto(this, dpiAlto)
                PrefsManager.setDpiNormal(this, dpiNormal)

                if (DpiUtils.setDensity(this, dpiAlto)) {
                    trickAtivo = true
                } else {
                    avisarFalha()
                }
            } else {
                val dpiNormal = PrefsManager.getDpiNormal(this, dpiOriginal)
                if (DpiUtils.setDensity(this, dpiNormal)) {
                    trickAtivo = false
                } else {
                    avisarFalha()
                }
            }
            PrefsManager.setTrickAtivo(this, trickAtivo)
            atualizarTextoBotaoTrick(btnTrick)
        }

        btnMinimizar.setOnClickListener {
            fullBar.visibility = View.GONE
            minimizedButton.visibility = View.VISIBLE
        }

        btnFechar.setOnClickListener {
            if (trickAtivo) {
                val dpiNormal = PrefsManager.getDpiNormal(this, dpiOriginal)
                DpiUtils.setDensity(this, dpiNormal)
                PrefsManager.setTrickAtivo(this, false)
            }
            stopSelf()
        }
    }

    private fun configurarBotaoMinimizado(view: View) {
        val fullBar = view.findViewById<View>(R.id.fullBar)
        val minimizedButton = view.findViewById<View>(R.id.minimizedButton)

        minimizedButton.setOnClickListener {
            minimizedButton.visibility = View.GONE
            fullBar.visibility = View.VISIBLE
        }

        habilitarArraste(minimizedButton)
    }

    private fun carregarValoresSalvos(view: View) {
        val inputDpiAlto = view.findViewById<EditText>(R.id.inputDpiAlto)
        val inputDpiNormal = view.findViewById<EditText>(R.id.inputDpiNormal)
        inputDpiAlto.setText(PrefsManager.getDpiAlto(this, 480).toString())
        inputDpiNormal.setText(PrefsManager.getDpiNormal(this, dpiOriginal).toString())
    }

    private fun habilitarFocoParaCampos(view: View) {
        val inputDpiAlto = view.findViewById<EditText>(R.id.inputDpiAlto)
        val inputDpiNormal = view.findViewById<EditText>(R.id.inputDpiNormal)

        val listener = View.OnFocusChangeListener { _, temFoco -> ajustarFocoDaJanela(temFoco) }
        inputDpiAlto.onFocusChangeListener = listener
        inputDpiNormal.onFocusChangeListener = listener
    }

    private fun ajustarFocoDaJanela(focavel: Boolean) {
        params.flags = if (focavel) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        overlayView?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun atualizarTextoBotaoTrick(btn: Button) {
        btn.text = if (trickAtivo) "Trick ON" else "Trick"
    }

    private fun avisarFalha() {
        Toast.makeText(
            this,
            "Não foi possível alterar o DPI. É necessário root ou a permissão WRITE_SECURE_SETTINGS concedida via ADB (veja o README).",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun habilitarArraste(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        isDragging = true
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    overlayView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // a view já pode ter sido removida
            }
        }
    }
}
