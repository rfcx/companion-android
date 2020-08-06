package org.rfcx.audiomoth.view

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.provider.AuthCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.entity.response.FirebaseAuthResponse
import org.rfcx.audiomoth.repo.ApiManager
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.util.CredentialKeeper
import org.rfcx.audiomoth.util.CredentialVerifier
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.util.Preferences.Companion.USER_FIREBASE_UID
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

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

        if (CredentialKeeper(this).hasValidCredentials()) {
            MainActivity.startActivity(this@LoginActivity, getDeploymentFromIntentIfHave(intent))
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
                            runOnUiThread { loading(false) }
                        }
                        is Ok -> {
                            saveUserToFirestore(result.value)
                        }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
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

    private fun saveUserToFirestore(result: UserAuthResponse) {
        val name = result.nickname ?: "Companion"
        val email = result.email ?: "Email"
        val user = User(name, email)

        Firestore(this@LoginActivity)
            .saveUser(user, result.guid) { string, isSuccess ->
                if (isSuccess) {
                    userTouch(result.idToken)
                    CredentialKeeper(this@LoginActivity).save(result)
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
                    exception.printStackTrace()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            saveUserToFirestore(result.value)
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
                override fun onFailure(dialog: Dialog) {}

                override fun onFailure(exception: AuthenticationException) {
                    Toast.makeText(this@LoginActivity, exception.description, Toast.LENGTH_SHORT)
                        .show()
                    loading(false)
                }

                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(this@LoginActivity).verify(credentials)) {
                        is Err -> {
                            Toast.makeText(this@LoginActivity, result.error, Toast.LENGTH_SHORT)
                                .show()
                            loading(false)
                        }
                        is Ok -> {
                            saveUserToFirestore(result.value)
                        }
                    }
                }
            })
    }

    private fun userTouch(idToken: String) {
        runOnUiThread { loading() }

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
                            getFirebaseAuth(authUser)
                        } else {
                            loading(false)
                        }
                    }
                }
            })
    }

    private fun getFirebaseAuth(authUser: String) {
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
                        signInWithFirebaseToken(it.firebaseToken)
                    }
                }
            })
    }

    private fun signInWithFirebaseToken(firebaseToken: String) {
        val preferences = Preferences.getInstance(this)

        auth.signInWithCustomToken(firebaseToken)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.uid?.let { preferences.putString(USER_FIREBASE_UID, it) }

                    MainActivity.startActivity(this@LoginActivity)
                    finish()
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
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getDeploymentFromIntentIfHave(intent)
        Log.d("notification", "LoginActivity onNewIntent")

    }

    private fun getDeploymentFromIntentIfHave(intent: Intent?) :String? {
        Log.d("notification", "LoginActivity getDeploymentFromIntentIfHave")

        if (intent?.hasExtra(MainActivity.EXTRA_EDGE_DEPLOYMENT_ID) == true) {
            return intent.getStringExtra(MainActivity.EXTRA_EDGE_DEPLOYMENT_ID)
        }
        return null
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        fun startActivity(context: Context) {
            Log.d("notification", "startActivity LoginActivity")

            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }
}
