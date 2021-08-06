package org.rfcx.companion.view

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import org.rfcx.companion.R
import org.rfcx.companion.entity.Err
import org.rfcx.companion.entity.Ok
import org.rfcx.companion.entity.UserAuthResponse
import org.rfcx.companion.util.CredentialVerifier
import org.rfcx.companion.util.Resource

class LoginViewModel(
    application: Application,
    private val loginRepository: LoginRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val loginWithEmailPassword = MutableLiveData<Resource<UserAuthResponse>>()


    private val auth0 by lazy {
        val auth0 =
            Auth0(
                context.getString(R.string.auth0_client_id),
                context.getString(R.string.auth0_domain)
            )
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

    fun login(email: String, password: String) {
        authentication.login(email, password, "Username-Password-Authentication")
            .setScope(context.getString(R.string.auth0_scopes))
            .setAudience(context.getString(R.string.auth0_audience))
            .start(object : BaseCallback<Credentials, AuthenticationException> {
                override fun onSuccess(credentials: Credentials) {
                    when (val result = CredentialVerifier(context).verify(credentials)) {
                        is Err -> {
                            loginWithEmailPassword.postValue(Resource.error(result.error, null))
                        }
                        is Ok -> {
                            loginWithEmailPassword.postValue(Resource.success(result.value))
                        }
                    }
                }

                override fun onFailure(exception: AuthenticationException) {
                    loginWithEmailPassword.postValue(
                        Resource.error(
                            context.getString(R.string.error_has_occurred),
                            null
                        )
                    )
                }
            })
    }

    fun loginWithEmailPassword(): LiveData<Resource<UserAuthResponse>> {
        return loginWithEmailPassword
    }


}
