package org.rfcx.audiomoth.view

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.result.Credentials
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Err
import org.rfcx.audiomoth.entity.Ok
import org.rfcx.audiomoth.util.CredentialVerifier

class LoginActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        signInButton.setOnClickListener {
            val email = loginEmailEditText.text.toString()
            val password = loginPasswordEditText.text.toString()
            it.hideKeyboard()

            if (validateInput(email, password)) {
                loading()
                login(email, password)
            }
        }
    }

    private fun login(email: String, password: String) {
        authentication.login(email, password, "Username-Password-Authentication")
            .setScope(this.getString(R.string.auth0_scopes))
            .setAudience(this.getString(R.string.auth0_audience))
            .start(object : BaseCallback<Credentials, AuthenticationException> {
                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                        }
                        is Ok -> {
                            MainActivity.startActivity(this@LoginActivity, result.value.guid)
                        }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
                    exception.printStackTrace()
                }
            })
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

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}
