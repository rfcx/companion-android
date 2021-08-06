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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.response.FirebaseAuthResponse
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.*
import org.rfcx.companion.util.Preferences.Companion.DISPLAY_THEME
import org.rfcx.companion.util.Preferences.Companion.USER_FIREBASE_UID
import org.rfcx.companion.view.project.ProjectSelectActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val analytics by lazy { Analytics(this) }
    private lateinit var loginViewModel: LoginViewModel

    private val auth0 by lazy {
        val auth0 =
            Auth0(this.getString(R.string.auth0_client_id), this.getString(R.string.auth0_domain))
        auth0.isOIDCConformant = true
        auth0.isLoggingEnabled = true
        auth0
    }

    private val webAuthentication by lazy {
        WebAuthProvider.init(auth0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        setViewModel()
        setObserver()
        setupDisplayTheme()

        if (CredentialKeeper(this).hasValidCredentials()) {
            ProjectSelectActivity.startActivity(this@LoginActivity)
            finish()
        }

        signInButton.setOnClickListener {
            val email = loginEmailEditText.text.toString()
            val password = loginPasswordEditText.text.toString()
            it.hideKeyboard()

            if (validateInput(email, password)) {
                loading()
                loginViewModel.login(email, password)
            }
        }

        facebookLoginButton.setOnClickListener {
            loading()
            loginWithFacebook()
        }

        googleLoginButton.setOnClickListener {
            loading()
            loginWithGoogle()
        }

        smsLoginButton.setOnClickListener {
            loading()
            loginMagicLink()
        }
    }

    private fun setViewModel() {
        loginViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(LoginViewModel::class.java)
    }

    private fun setObserver() {
        loginViewModel.loginWithEmailPassword().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {
                }
                Status.SUCCESS -> {
                    analytics.trackLoginEvent(LoginType.EMAIL.id, StatusEvent.SUCCESS.id)
                    it.data?.let { data ->
                        userTouch(data) // todo MVVM
                        CredentialKeeper(this@LoginActivity).save(data)
                    }
                }
                Status.ERROR -> {
                }
            }
        })
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

    private fun loginWithFacebook() {
        webAuthentication
            .withConnection("facebook")
            .withScope(this.getString(R.string.auth0_scopes))
            .withScheme(this.getString(R.string.auth0_scheme))
            .withAudience(this.getString(R.string.auth0_audience))
            .start(this, object : AuthCallback {
                override fun onFailure(dialog: Dialog) {}

                override fun onFailure(exception: AuthenticationException) {
                    analytics.trackLoginEvent(LoginType.FACEBOOK.id, StatusEvent.FAILURE.id)

                    exception.printStackTrace()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            analytics.trackLoginEvent(LoginType.FACEBOOK.id, StatusEvent.FAILURE.id)

                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            analytics.trackLoginEvent(LoginType.FACEBOOK.id, StatusEvent.SUCCESS.id)

                            userTouch(result.value)
                            CredentialKeeper(this@LoginActivity).save(result.value)
                        }
                    }
                }
            })
    }

    private fun loginWithGoogle() {
        webAuthentication
            .withConnection("google-oauth2")
            .withScope(this.getString(R.string.auth0_scopes))
            .withScheme(this.getString(R.string.auth0_scheme))
            .withAudience(this.getString(R.string.auth0_audience))
            .start(this, object : AuthCallback {
                override fun onFailure(dialog: Dialog) {}

                override fun onFailure(exception: AuthenticationException) {
                    analytics.trackLoginEvent(LoginType.GOOGLE.id, StatusEvent.FAILURE.id)

                    exception.printStackTrace()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            analytics.trackLoginEvent(LoginType.GOOGLE.id, StatusEvent.FAILURE.id)

                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            analytics.trackLoginEvent(LoginType.GOOGLE.id, StatusEvent.SUCCESS.id)

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
                    analytics.trackLoginEvent(LoginType.SMS.id, StatusEvent.FAILURE.id)
                }

                override fun onFailure(exception: AuthenticationException) {
                    analytics.trackLoginEvent(LoginType.SMS.id, StatusEvent.FAILURE.id)

                    Toast.makeText(this@LoginActivity, exception.description, Toast.LENGTH_SHORT)
                        .show()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            analytics.trackLoginEvent(LoginType.SMS.id, StatusEvent.FAILURE.id)

                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            analytics.trackLoginEvent(LoginType.SMS.id, StatusEvent.SUCCESS.id)

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
                        ProjectSelectActivity.startActivity(this@LoginActivity)
                        finish()
                    }
                } else {
                    loading(false)
                    Toast.makeText(baseContext, R.string.an_error_occurred, Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun loading(start: Boolean = true) {
        loginGroupView.visibility = if (start) View.INVISIBLE else View.VISIBLE
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
