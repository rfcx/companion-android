package org.rfcx.companion.view

import android.content.Context
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.databinding.ActivityLoginBinding
import org.rfcx.companion.entity.*
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.*
import org.rfcx.companion.util.Preferences.Companion.DISPLAY_THEME
import org.rfcx.companion.view.project.ProjectSelectActivity

class LoginActivity : AppCompatActivity() {

    private val analytics by lazy { Analytics(this) }
    private lateinit var loginViewModel: LoginViewModel
    private var userAuthResponse: UserAuthResponse? = null
    private val firebaseCrashlytics by lazy { Crashlytics() }
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setViewModel()
        setObserver()
        setupDisplayTheme()

        if (this.getIdToken() != null && loginViewModel.getSelectedProject() != -1) {
            ProjectSelectActivity.startActivity(this@LoginActivity)
            finish()
        }

        binding.signInButton.setOnClickListener {
            val email = binding.loginEmailEditText.text.toString().trim()
            val password = binding.loginPasswordEditText.text.toString()
            firebaseCrashlytics.setCustomKey(CrashlyticsKey.LoginWith.key, email)
            it.hideKeyboard()

            if (validateInput(email, password)) {
                if (this.isNetworkAvailable()) {
                    loading()
                    loginViewModel.login(email, password)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.no_internet_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.googleLoginButton.setOnClickListener {
            loading()
            loginViewModel.loginWithGoogle(this)
        }
    }

    private fun setViewModel() {
        loginViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl(this)),
                CoreApiHelper(CoreApiServiceImpl(this)),
                LocalDataHelper()
            )
        ).get(LoginViewModel::class.java)
    }

    private fun setObserver() {
        loginViewModel.loginWithEmailPassword().observe(
            this,
            Observer {
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
                                it.message ?: getString(R.string.login_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

        loginViewModel.loginWithGoogleState().observe(
            this,
            Observer {
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
            }
        )

        loginViewModel.userTouchState().observe(
            this,
            Observer {
                when (it.status) {
                    Status.LOADING -> {}
                    Status.SUCCESS -> {
                        it.data?.let { data ->
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
            }
        )
    }

    private val Context.isDarkMode
        get() = if (getDefaultNightMode() == MODE_NIGHT_FOLLOW_SYSTEM)
            resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
        else getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

    private fun setupDisplayTheme() {
        val preferences = Preferences.getInstance(this)
        val themeOption = this.resources.getStringArray(R.array.theme_more_than_9)
        var defaultTheme = themeOption[1]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            defaultTheme = themeOption[2]
        }
        preferences.putString(DISPLAY_THEME, defaultTheme)

        val theme = when (preferences.getString(DISPLAY_THEME, defaultTheme)) {
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
        binding.loginGroupView.visibility = if (start) View.INVISIBLE else View.VISIBLE
        binding.loginProgressBar.visibility = if (start) View.VISIBLE else View.GONE
    }

    private fun validateInput(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty()) {
            binding.loginEmailEditText.error = getString(R.string.pls_fill_email)
            return false
        } else if (password.isNullOrEmpty()) {
            binding.loginPasswordEditText.error = getString(R.string.pls_fill_password)
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
