package com.lifeos.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.lifeos.app.ui.LifeOsAppShell
import com.lifeos.ui.theme.LifeOsMaterialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sharedUriState = mutableStateOf<Uri?>(null)
    private val sharedTextState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permissão processada pelo sistema */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        extractIncomingShareIntent(intent)

        setContent {
            LifeOsMaterialTheme {
                LifeOsAppShell(
                    incomingSharedUri = sharedUriState.value,
                    incomingSharedText = sharedTextState.value,
                    onConsumedSharedPayload = {
                        sharedUriState.value = null
                        sharedTextState.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingShareIntent(intent)
    }

    private fun extractIncomingShareIntent(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND) return
        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            incoming.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        val extraText = incoming.getStringExtra(Intent.EXTRA_TEXT)

        when {
            streamUri != null -> sharedUriState.value = streamUri
            !extraText.isNullOrBlank() -> sharedTextState.value = extraText
        }
    }
}
