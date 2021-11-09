package org.rfcx.companion.view

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
            loginViewModel.loginMagicLink(this)
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
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        loginViewModel.loginWithPhoneNumberState().observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    analytics.trackLoginEvent(LoginType.SMS.id, StatusEvent.SUCCESS.id)
                    it.data?.let { data ->
                        runOnUiThread { loading() }
                        this.userAuthResponse = data
                        loginViewModel.userTouch(data)
                        CredentialKeeper(this@LoginActivity).save(data)
                    }
                }
                Status.ERROR -> {
                    analytics.trackLoginEvent(LoginType.SMS.id, StatusEvent.FAILURE.id)
                    loading(false)
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            it.message ?: getString(R.string.error_has_occurred),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
