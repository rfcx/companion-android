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
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.*
import org.rfcx.companion.util.Preferences.Companion.DISPLAY_THEME
import org.rfcx.companion.util.Preferences.Companion.USER_FIREBASE_UID
import org.rfcx.companion.view.project.ProjectSelectActivity

class LoginActivity : AppCompatActivity() {

    private val analytics by lazy { Analytics(this) }
    private lateinit var loginViewModel: LoginViewModel

    private var userAuthResponse: UserAuthResponse? = null

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
            loginViewModel.loginWithFacebook(this)
        }

        googleLoginButton.setOnClickListener {
            loading()
            loginViewModel.loginWithGoogle(this)
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
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    analytics.trackLoginEvent(LoginType.EMAIL.id, StatusEvent.SUCCESS.id)
                    it.data?.let { data ->
                        runOnUiThread { loading() }
                        this.userAuthResponse = data
                        loginViewModel.userTouch(data)
                        CredentialKeeper(this@LoginActivity).save(data)
                    }
                }
                Status.ERROR -> {
                    analytics.trackLoginEvent(LoginType.EMAIL.id, StatusEvent.FAILURE.id)
                    loading(false)

                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: getString(R.string.error_has_occurred),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        loginViewModel.loginWithFacebookState().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    analytics.trackLoginEvent(LoginType.FACEBOOK.id, StatusEvent.SUCCESS.id)
                    it.data?.let { data ->
                        runOnUiThread { loading() }
                        this.userAuthResponse = data
                        loginViewModel.userTouch(data)
                        CredentialKeeper(this@LoginActivity).save(data)
                    }
                }
                Status.ERROR -> {
                    analytics.trackLoginEvent(LoginType.FACEBOOK.id, StatusEvent.FAILURE.id)
                    loading(false)

                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: getString(R.string.error_has_occurred),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        loginViewModel.loginWithGoogleState().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    analytics.trackLoginEvent(LoginType.GOOGLE.id, StatusEvent.SUCCESS.id)
                    it.data?.let { data ->
                        runOnUiThread { loading() }
                        this.userAuthResponse = data
                        loginViewModel.userTouch(data)
                        CredentialKeeper(this@LoginActivity).save(data)
                    }
                }
                Status.ERROR -> {
                    analytics.trackLoginEvent(LoginType.GOOGLE.id, StatusEvent.FAILURE.id)
                    loading(false)

                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: getString(R.string.error_has_occurred),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        loginViewModel.userTouchState().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    it.data?.let { data ->
                        loginViewModel.getFirebaseAuth(data)
                    }
                }
                Status.ERROR -> {
                    loading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: getString(R.string.error_has_occurred),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        loginViewModel.firebaseAuthState().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    it.data?.let { token ->
                        loginViewModel.signInWithFirebaseToken(this, token)
                    }
                }
                Status.ERROR -> {
                    loading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: getString(R.string.error_has_occurred),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        loginViewModel.signInWithFirebaseTokenState().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    it.data?.let { uid ->
                        val preferences = Preferences.getInstance(this)
                        preferences.putString(USER_FIREBASE_UID, uid)
                        ProjectSelectActivity.startActivity(this@LoginActivity)
                        finish()
                    }
                }
                Status.ERROR -> {
                    loading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: getString(R.string.error_has_occurred),
                        Toast.LENGTH_SHORT
                    ).show()
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
                            runOnUiThread { loading() }
                            loginViewModel.userTouch(result.value)
                            CredentialKeeper(this@LoginActivity).save(result.value)
                        }
                    }
                }
            })
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
