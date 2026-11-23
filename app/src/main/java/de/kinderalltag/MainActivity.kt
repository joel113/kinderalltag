package de.kinderalltag

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import de.kinderalltag.text.*
import de.kinderalltag.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppStrings.activityCreated()
        de.kinderalltag.notifications.ReminderScheduler.createChannel(this)
        de.kinderalltag.notifications.ActivityNotifications.createChannel(this)
        enableEdgeToEdge()
        if (savedInstanceState == null) receiveCalendar(intent)
        setContent {
            AppThemeHost {
                val repository = (application as KinderApplication).repository
                if (repository == null) {
                    SetupScreen()
                } else {
                    val vm: FamilyViewModel =
                        viewModel(
                            factory =
                                object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return FamilyViewModel(repository) as T
                                    }
                                }
                        )
                    val scope = rememberCoroutineScope()
                    var loginError by remember { mutableStateOf<String?>(null) }
                    var signingIn by remember { mutableStateOf(false) }
                    KinderApp(
                        vm,
                        intent.getStringExtra("date"),
                        intent.getStringExtra("occurrence"),
                        signingIn,
                        loginError,
                        onSignIn = {
                            scope.launch {
                                signingIn = true
                                loginError = null
                                var stage = text(TextKey.CONFIGURATION)
                                try {
                                    val clientIdResource =
                                        resources.getIdentifier(
                                            "default_web_client_id",
                                            "string",
                                            packageName,
                                        )
                                    check(clientIdResource != 0) {
                                        text(
                                            TextKey
                                                .DEFAULT_WEB_CLIENT_ID_IS_MISSING_FROM_THE_FIREBASE_CONFIGURATION
                                        )
                                    }
                                    val option =
                                        GetSignInWithGoogleOption.Builder(
                                                getString(clientIdResource)
                                            )
                                            .build()
                                    stage = text(TextKey.GOOGLE_ACCOUNT_SELECTION)
                                    val result =
                                        CredentialManager.create(this@MainActivity)
                                            .getCredential(
                                                this@MainActivity,
                                                GetCredentialRequest.Builder()
                                                    .addCredentialOption(option)
                                                    .build(),
                                            )
                                    val credential = result.credential
                                    stage = text(TextKey.GOOGLE_CREDENTIALS)
                                    check(
                                        credential is CustomCredential &&
                                            credential.type ==
                                                GoogleIdTokenCredential
                                                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    )
                                    val token =
                                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    stage = text(TextKey.FIREBASE_SIGN_IN)
                                    FirebaseAuth.getInstance()
                                        .signInWithCredential(
                                            GoogleAuthProvider.getCredential(token, null)
                                        )
                                        .await()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.e(
                                        "KinderalltagAuth",
                                        text(TextKey.GOOGLE_SIGN_IN_FAILED, stage),
                                        e,
                                    )
                                    loginError =
                                        if (stage == text(TextKey.CONFIGURATION)) {
                                            text(
                                                TextKey
                                                    .GOOGLE_SIGN_IN_IS_NOT_CONFIGURED_YET_ENABLE_GOOGLE_IN_FIREBASE_AUTHENTIC
                                            )
                                        } else {
                                            text(
                                                TextKey
                                                    .SIGN_IN_WAS_NOT_COMPLETED_CHECK_GOOGLE_CONFIGURATION_INTERNET_AND_SIGNIN,
                                                stage,
                                            )
                                        }
                                } finally {
                                    signingIn = false
                                }
                            }
                        },
                        onSignOut = {
                            repository.signOut()
                            scope.launch {
                                runCatching {
                                    CredentialManager.create(this@MainActivity)
                                        .clearCredentialState(ClearCredentialStateRequest())
                                }
                            }
                        },
                        initialPage = intent.getStringExtra("openPage"),
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun receiveCalendar(incoming: Intent) {
        val inbox = (application as KinderApplication).calendarInbox
        try {
            val uris: List<Uri> =
                when (incoming.action) {
                    Intent.ACTION_VIEW -> listOfNotNull(incoming.data)
                    Intent.ACTION_SEND ->
                        listOfNotNull(
                            (incoming.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)
                                as? Uri)
                                ?: incoming.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                        )
                    Intent.ACTION_SEND_MULTIPLE ->
                        incoming
                            .getParcelableArrayListExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)
                            ?.filterIsInstance<Uri>()
                            .orEmpty()
                    else -> return
                }
            val text = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            inbox.receive(uris, text?.takeIf { uris.isEmpty() })
        } catch (_: Exception) {
            inbox.reportError(text(TextKey.THE_SHARED_CALENDAR_FILE_COULD_NOT_BE_OPENED))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveCalendar(intent)
    }
}

@Composable
private fun SetupScreen() {
    AppBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Kinderalltag", style = MaterialTheme.typography.headlineLarge)
            AppBanner(
                text(TextKey.A_SHARED_VIEW_OF_YOUR_DAY),
                when (AppTheme.definition.id) {
                    AppThemeId.GARDEN -> text(TextKey.LITTLE_TASKS_FOR_YOUR_FAMILY_GARDEN)
                    AppThemeId.PLAY -> text(TextKey.PLAYING_THROUGH_YOUR_DAY_TOGETHER)
                    AppThemeId.SPACE -> text(TextKey.ON_AN_EVERYDAY_MISSION_TOGETHER)
                    AppThemeId.ANIMALS ->
                        text(TextKey.AN_ANIMAL_ADVENTURE_THROUGH_YOUR_DAY_TOGETHER)
                    AppThemeId.VEHICLES -> text(TextKey.ROLLING_THROUGH_EVERYDAY_LIFE_TOGETHER)
                    AppThemeId.NATURE -> text(TextKey.GROWING_THROUGH_YOUR_DAY_TOGETHER)
                    AppThemeId.FARM -> text(TextKey.REAPING_THE_REWARDS_OF_FAMILY_LIFE_TOGETHER)
                    AppThemeId.MAGIC -> text(TextKey.A_LITTLE_EVERYDAY_MAGIC_FOR_YOUR_FAMILY)
                },
            )
            Text(
                text(
                    TextKey.FIREBASE_IS_NOT_CONFIGURED_YET_ADD_GOOGLE_SERVICES_JSON_TO_THE_APP_FOLDE
                )
            )
        }
    }
}
