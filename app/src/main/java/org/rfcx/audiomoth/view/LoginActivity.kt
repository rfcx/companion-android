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
import org.rfcx.audiomoth.entity.UserTouchResponse
import org.rfcx.audiomoth.repo.ApiManager
import org.rfcx.audiomoth.util.CredentialKeeper
import org.rfcx.audiomoth.util.CredentialVerifier
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                            loading(false)
                        }
                        is Ok -> {
                            userTouch(result.value.idToken)
                            CredentialKeeper(this@LoginActivity).save(result.value)
                        }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
                    exception.printStackTrace()
                    loading(false)
                }
            })
    }

    private fun userTouch(idToken: String) {
        val authUser = "Bearer $idToken"
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
                            MainActivity.startActivity(this@LoginActivity)
                            finish()
                        }
                    }
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
