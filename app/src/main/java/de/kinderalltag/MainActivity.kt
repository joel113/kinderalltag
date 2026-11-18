package de.kinderalltag

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.ClearCredentialStateRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import de.kinderalltag.ui.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) receiveCalendar(intent)
        setContent {
            KinderTheme {
                val repository = (application as KinderApplication).repository
                if (repository == null) {
                    SetupScreen()
                } else {
                    val vm: FamilyViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return FamilyViewModel(repository) as T
                        }
                    })
                    val scope = rememberCoroutineScope()
                    var loginError by remember { mutableStateOf<String?>(null) }
                    var signingIn by remember { mutableStateOf(false) }
                    KinderApp(vm, intent.getStringExtra("date"), intent.getStringExtra("occurrence"),
                        signingIn, loginError,
                        onSignIn = {
                            scope.launch {
                                signingIn = true; loginError = null
                                var stage = "Konfiguration"
                                try {
                                    val clientIdResource = resources.getIdentifier("default_web_client_id", "string", packageName)
                                    check(clientIdResource != 0) { "default_web_client_id fehlt in der Firebase-Konfiguration." }
                                    val option = GetSignInWithGoogleOption.Builder(getString(clientIdResource)).build()
                                    stage = "Google-Kontoauswahl"
                                    val result = CredentialManager.create(this@MainActivity).getCredential(
                                        this@MainActivity, GetCredentialRequest.Builder().addCredentialOption(option).build())
                                    val credential = result.credential
                                    stage = "Google-Anmeldedaten"
                                    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    stage = "Firebase-Anmeldung"
                                    FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.e("KinderalltagAuth", "Google-Anmeldung fehlgeschlagen: $stage", e)
                                    loginError = if (stage == "Konfiguration") {
                                        "Google-Anmeldung ist noch nicht konfiguriert. Google in Firebase Authentication aktivieren und die aktualisierte google-services.json einbauen."
                                    } else {
                                        "Anmeldung nicht abgeschlossen ($stage). Google-Konfiguration, Internet und Signatur-Fingerabdruck prüfen."
                                    }
                                } finally { signingIn = false }
                            }
                        },
                        onSignOut = {
                            repository.signOut()
                            scope.launch {
                                runCatching { CredentialManager.create(this@MainActivity).clearCredentialState(ClearCredentialStateRequest()) }
                            }
                        }, initialPage = intent.getStringExtra("openPage"))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun receiveCalendar(incoming: Intent) {
        val inbox = (application as KinderApplication).calendarInbox
        try {
            val uris: List<Uri> = when (incoming.action) {
                Intent.ACTION_VIEW -> listOfNotNull(incoming.data)
                Intent.ACTION_SEND -> listOfNotNull((incoming.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                    ?: incoming.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri)
                Intent.ACTION_SEND_MULTIPLE -> incoming.getParcelableArrayListExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>().orEmpty()
                else -> return
            }
            val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            inbox.receive(uris, text?.takeIf { uris.isEmpty() })
        } catch (_: Exception) { inbox.reportError("Die geteilte Kalenderdatei konnte nicht geöffnet werden.") }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveCalendar(intent)
    }
}

@Composable
private fun SetupScreen() {
    GardenBackground(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Kinderalltag", style = MaterialTheme.typography.headlineLarge)
            GardenBanner("Ein gemeinsamer Blick auf euren Tag.", "Kleine Aufgaben für euren Familiengarten.")
            Text("Die Firebase-Verbindung ist noch nicht eingerichtet. Bitte google-services.json im app-Ordner ergänzen und die App neu bauen. Die Anleitung steht in docs/SETUP.md.")
        }
    }
}
