package org.rfcx.companion.view

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.companion.MainActivity
import org.rfcx.companion.R
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.response.FirebaseAuthResponse
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.repo.Firestore
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.CredentialKeeper
import org.rfcx.companion.util.CredentialVerifier
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Preferences.Companion.DISPLAY_THEME
import org.rfcx.companion.util.Preferences.Companion.USER_FIREBASE_UID
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val analytics by lazy { Analytics(this) }

    private val auth0 by lazy {
        val auth0 =
            Auth0(this.getString(R.string.auth0_client_id), this.getString(R.string.auth0_domain))
        auth0.isOIDCConformant = true
        auth0.isLoggingEnabled = true
        auth0
    }

    private val authentication by lazy {
        AuthenticationAPIClient(auth0)
    }

    private val webAuthentication by lazy {
        WebAuthProvider.init(auth0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        setupDisplayTheme()

        if (CredentialKeeper(this).hasValidCredentials()) {
            MainActivity.startActivity(this@LoginActivity)
            finish()
        }

        signInButton.setOnClickListener {
            val email = loginEmailEditText.text.toString()
            val password = loginPasswordEditText.text.toString()
            it.hideKeyboard()

            if (validateInput(email, password)) {
                loading()
                login(email, password)
            }
        }

        facebookLoginButton.setOnClickListener {
            loading()
            loginWithFacebook()
        }

        smsLoginButton.setOnClickListener {
            loading()
            loginMagicLink()
        }
    }

    private fun setupDisplayTheme() {
        val preferences = Preferences.getInstance(this)
        val themeOption = this.resources.getStringArray(R.array.theme_more_than_9)
        val theme = when (preferences.getString(DISPLAY_THEME, themeOption[1])) {
            themeOption[0] -> {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            themeOption[1] -> {
                AppCompatDelegate.MODE_NIGHT_YES
            }
            else -> {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
        AppCompatDelegate.setDefaultNightMode(theme)
    }

    private fun login(email: String, password: String) {
        authentication.login(email, password, "Username-Password-Authentication")
            .setScope(this.getString(R.string.auth0_scopes))
            .setAudience(this.getString(R.string.auth0_audience))
            .start(object : BaseCallback<Credentials, AuthenticationException> {
                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            analytics.trackLoginEvent(LoginType.EMAIL.id, Status.FAILURE.id)

                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            runOnUiThread { loading(false) }
                        }
                        is Ok -> {
                            analytics.trackLoginEvent(LoginType.EMAIL.id, Status.SUCCESS.id)

                            userTouch(result.value)
                            CredentialKeeper(this@LoginActivity).save(result.value)
                        }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
                    analytics.trackLoginEvent(LoginType.EMAIL.id, Status.FAILURE.id)

                    exception.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        loading(false)
                    }
                }
            })
    }

    private fun saveUserToFirestore(result: UserAuthResponse, uid: String) {
        val name = result.nickname ?: "Companion"
        val email = result.email ?: "Email"
        val idToken = result.idToken
        val user = User(name, email, idToken)

        Firestore(this@LoginActivity)
            .saveUser(user, uid) { string, isSuccess ->
                if (isSuccess) {
                    MainActivity.startActivity(this@LoginActivity)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, string, Toast.LENGTH_SHORT).show()
                    loading(false)
                }
            }
    }

    private fun loginWithFacebook() {
        webAuthentication
            .withConnection("facebook")
            .withScope(this.getString(R.string.auth0_scopes))
            .withScheme(this.getString(R.string.auth0_scheme))
            .withAudience(this.getString(R.string.auth0_audience))
            .start(this, object : AuthCallback {
                override fun onFailure(dialog: Dialog) {}

                override fun onFailure(exception: AuthenticationException) {
                    analytics.trackLoginEvent(LoginType.FACEBOOK.id, Status.FAILURE.id)

                    exception.printStackTrace()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            analytics.trackLoginEvent(LoginType.FACEBOOK.id, Status.FAILURE.id)

                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            analytics.trackLoginEvent(LoginType.FACEBOOK.id, Status.SUCCESS.id)

                            userTouch(result.value)
                            CredentialKeeper(this@LoginActivity).save(result.value)
                        }
                    }
                }
            })
    }

    private fun loginMagicLink() {
        webAuthentication
            .withConnection("")
            .withScope(this.getString(R.string.auth0_scopes))
            .withScheme(this.getString(R.string.auth0_scheme))
            .withAudience(this.getString(R.string.auth0_audience))
            .start(this, object : AuthCallback {
                override fun onFailure(dialog: Dialog) {
                    analytics.trackLoginEvent(LoginType.SMS.id, Status.FAILURE.id)
                }

                override fun onFailure(exception: AuthenticationException) {
                    analytics.trackLoginEvent(LoginType.SMS.id, Status.FAILURE.id)

                    Toast.makeText(this@LoginActivity, exception.description, Toast.LENGTH_SHORT)
                        .show()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            analytics.trackLoginEvent(LoginType.SMS.id, Status.FAILURE.id)

                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            analytics.trackLoginEvent(LoginType.SMS.id, Status.SUCCESS.id)

                            userTouch(result.value)
                            CredentialKeeper(this@LoginActivity).save(result.value)
                        }
                    }
                }
            })
    }

    private fun userTouch(result: UserAuthResponse) {
        runOnUiThread { loading() }

        val authUser = "Bearer ${result.idToken}"
        ApiManager.getInstance().apiRest.userTouch(authUser)
            .enqueue(object : Callback<UserTouchResponse> {
                override fun onFailure(call: Call<UserTouchResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT).show()
                    loading(false)
                }

                override fun onResponse(
                    call: Call<UserTouchResponse>,
                    response: Response<UserTouchResponse>
                ) {
                    response.body()?.let {
                        if (it.success) {
                            getFirebaseAuth(authUser, result)
                        } else {
                            loading(false)
                        }
                    }
                }
            })
    }

    private fun getFirebaseAuth(authUser: String, result: UserAuthResponse) {
        ApiManager.getInstance().apiFirebaseAuth.firebaseAuth(authUser)
            .enqueue(object : Callback<FirebaseAuthResponse> {
                override fun onFailure(call: Call<FirebaseAuthResponse>, t: Throwable) {
                    loading(false)
                    Toast.makeText(baseContext, R.string.an_error_occurred, Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onResponse(
                    call: Call<FirebaseAuthResponse>,
                    response: Response<FirebaseAuthResponse>
                ) {
                    response.body()?.let {
                        signInWithFirebaseToken(it.firebaseToken, result)
                    }
                }
            })
    }

    private fun signInWithFirebaseToken(firebaseToken: String, result: UserAuthResponse) {
        val preferences = Preferences.getInstance(this)

        auth.signInWithCustomToken(firebaseToken)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.uid?.let {
                        preferences.putString(USER_FIREBASE_UID, it)
                        saveUserToFirestore(result, it)
                    }
                } else {
                    loading(false)
                    Toast.makeText(baseContext, R.string.an_error_occurred, Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun loading(start: Boolean = true) {
        loginGroupView.visibility = if (start) View.GONE else View.VISIBLE
        loginProgressBar.visibility = if (start) View.VISIBLE else View.GONE
    }

    private fun validateInput(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty()) {
            loginEmailEditText.error = getString(R.string.pls_fill_email)
            return false
        } else if (password.isNullOrEmpty()) {
            loginPasswordEditText.error = getString(R.string.pls_fill_password)
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        loading(false)
        analytics.trackScreen(Screen.LOGIN)
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }
}
