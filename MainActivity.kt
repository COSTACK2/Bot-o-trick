package com.example.dpitrick

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnPermissao: Button
    private lateinit var btnIniciar: Button

    companion object {
        private const val REQUEST_CODE_OVERLAY = 1001
        private const val REQUEST_CODE_NOTIF = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnPermissao = findViewById(R.id.btnPermissao)
        btnIniciar = findViewById(R.id.btnIniciar)

        btnPermissao.setOnClickListener { pedirPermissoes() }
        btnIniciar.setOnClickListener { iniciarOverlay() }
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus()
    }

    private fun atualizarStatus() {
        val temOverlay = Settings.canDrawOverlays(this)
        statusText.text = if (temOverlay) {
            getString(R.string.status_permissao_ok)
        } else {
            getString(R.string.status_permissao_faltando)
        }
        btnIniciar.isEnabled = temOverlay
    }

    private fun pedirPermissoes() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        } else {
            Toast.makeText(this, R.string.status_permissao_ok, Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIF
                )
            }
        }
    }

    private fun iniciarOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.status_permissao_faltando, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, R.string.overlay_iniciado, Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            atualizarStatus()
        }
    }
}
