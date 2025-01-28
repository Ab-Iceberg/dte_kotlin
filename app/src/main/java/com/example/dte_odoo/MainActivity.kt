package com.example.dte_odoo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
class MainActivity : ComponentActivity() {

    private val STORAGE_PERMISSION_CODE = 1
    private lateinit var myWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (!verificarPermisosAlmacenar()) {
            solicitarPermisosAlmacenar()
        }

        val myWebView: WebView = findViewById(R.id.webview_id)
        myWebView.apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    view?.loadUrl(request?.url.toString())
                    return true
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                if (verificarPermisosAlmacenar()) {
                    val fileName = contentDisposition?.substringAfter("filename=")?.replace("\"", "")
                        ?: Uri.parse(url).lastPathSegment

                    val filePath = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setTitle(fileName)
                        setDescription("Descargando archivo...")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationUri(Uri.fromFile(filePath))
                        addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                        addRequestHeader("User-Agent", userAgent)
                    }

                    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    val downloadId = downloadManager.enqueue(request)
                    alertaDescarga(filePath.absolutePath)

                    Toast.makeText(
                        this@MainActivity,
                        "Descargando archivo: $fileName",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Permisos de almacenamiento no concedidos.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            loadUrl("http://190.186.18.34:805")
        }
    }

    private fun alertaDescarga(filePath: String) {
        val constructor = AlertDialog.Builder(this)
        constructor.setTitle("Descarga Completa")
        constructor.setMessage("El archivo se ha descargado correctamente en:\n$filePath")
        constructor.setPositiveButton("Aceptar") { dialog, _ ->
            dialog.dismiss()
        }
        constructor.create().show()
    }

    private fun solicitarPermisosAlmacenar() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            Toast.makeText(
                this,
                "Se necesitan permisos para almacenar archivos descargados.",
                Toast.LENGTH_LONG
            ).show()
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun verificarPermisosAlmacenar(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos denegados. No se podrán descargar archivos.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
