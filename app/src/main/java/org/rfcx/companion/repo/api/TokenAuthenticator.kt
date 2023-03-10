package org.rfcx.companion.repo.api

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.result.Credentials
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.rfcx.companion.R
import org.rfcx.companion.entity.Err
import org.rfcx.companion.entity.Ok
import org.rfcx.companion.util.CredentialKeeper
import org.rfcx.companion.util.CredentialVerifier
import org.rfcx.companion.util.Preferences
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TokenAuthenticator(private val context: Context) : Authenticator {

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

    override fun authenticate(route: Route?, response: Response): Request? {

        var refreshResult = false
        runBlocking {
            refreshResult = refreshToken()
        }
        return if (refreshResult) {
            // refresh token is successful, we saved new token to storage.
            // Get your token from storage and set header
            val token = Preferences.getInstance(context).getString(Preferences.ID_TOKEN)

            // execute failed request again with new access token
            response.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            // Refresh token failed, you can logout user or retry couple of times
            // Returning null is critical here, it will stop the current request
            // If you do not return null, you will end up in a loop calling refresh
            null
        }
    }

    private suspend fun refreshToken(): Boolean {
        val credentialKeeper = CredentialKeeper(context)
        val credentialVerifier = CredentialVerifier(context)
        val refreshToken = Preferences.getInstance(context).getString(Preferences.REFRESH_TOKEN)
        val token = Preferences.getInstance(context).getString(Preferences.ID_TOKEN)

        if (refreshToken == null) {
            return false
        }
        if (token == null) {
            return false
        }
        if (credentialKeeper.hasValidCredentials()) {
            return true
        }

        return suspendCoroutine { cont ->
            authentication.renewAuth(refreshToken).start(object : BaseCallback<Credentials, AuthenticationException> {
                override fun onSuccess(credentials: Credentials?) {
                    if (credentials == null) cont.resume(false)

                    val result = credentialVerifier.verify(credentials!!)
                    when (result) {
                        is Err -> {
                            cont.resume(false)
                        }
                        is Ok -> {
                            val userAuthResponse = result.value
                            credentialKeeper.save(userAuthResponse)
                            cont.resume(true)
                        }
                    }
                }

                override fun onFailure(error: AuthenticationException) {
                    cont.resume(false)
                }
            })
        }
    }
}
