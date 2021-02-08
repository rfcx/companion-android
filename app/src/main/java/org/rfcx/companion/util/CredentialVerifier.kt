package org.rfcx.companion.util

import android.content.Context
import com.auth0.android.result.Credentials
import io.jsonwebtoken.Jwts
import org.rfcx.companion.R
import org.rfcx.companion.entity.Err
import org.rfcx.companion.entity.Ok
import org.rfcx.companion.entity.Result
import org.rfcx.companion.entity.UserAuthResponse

/**
 * Takes an Auth0 credential object and returns the parsed response
 */

class CredentialVerifier(val context: Context) {

    fun verify(credentials: Credentials): Result<UserAuthResponse, String> {
        val token = credentials.idToken
        if (token == null) {
            return Err(getString(R.string.an_error_occurred))
        }

        // Parsing JWT Token
        val metaDataKey = getString(R.string.auth0_metadata_key)
        val withoutSignature = token.substring(0, token.lastIndexOf('.') + 1)
        try {
            val untrusted = Jwts.parser().parseClaimsJwt(withoutSignature)
            if (untrusted.body[metaDataKey] == null) {
                return Err(getString(R.string.an_error_occurred))
            }

            val metadata = untrusted.body[metaDataKey]
            if (metadata == null || !(metadata is HashMap<*, *>)) {
                return Err(getString(R.string.an_error_occurred))
            }

            var name: String?
            name = if (untrusted.body["given_name"] != null) {
                untrusted.body["given_name"] as String?
            } else {
                untrusted.body["nickname"] as String?
            }

            val guid: String? = metadata["guid"] as String?
            val email: String? = untrusted.body["email"] as String?
            val picture: String? = untrusted.body["picture"] as String?
            val nickname: String? = name
            val defaultSite: String? = metadata["defaultSite"] as String?
            val expiresAt: Int = untrusted.body["exp"] as Int

            var accessibleSites: Set<String> = setOf()
            val accessibleSitesRaw = metadata["accessibleSites"]
            if (accessibleSitesRaw != null && accessibleSitesRaw is ArrayList<*> && accessibleSitesRaw.size > 0 && accessibleSitesRaw[0] is String) {
                accessibleSites =
                    HashSet<String>(accessibleSitesRaw as ArrayList<String>) // TODO: is there a better way to do this @anuphap @jingjoeh
            }

            var roles: Set<String> = setOf()
            val authorization = metadata["authorization"]
            if (authorization != null && authorization is HashMap<*, *>) {
                val rolesRaw = authorization["roles"]
                if (rolesRaw != null && rolesRaw is ArrayList<*> && rolesRaw.size > 0 && rolesRaw[0] is String) {
                    roles = HashSet<String>(rolesRaw as ArrayList<String>)
                }
            }

            when {
                guid.isNullOrEmpty() -> {
                    return Err(getString(R.string.an_error_occurred))
                }
                else -> {
                    return Ok(
                        UserAuthResponse(
                            guid,
                            email,
                            nickname,
                            token,
                            credentials.accessToken,
                            credentials.refreshToken,
                            roles,
                            accessibleSites,
                            defaultSite,
                            picture,
                            expiresAt.toLong()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Err(getString(R.string.an_error_occurred))
    }

    private fun getString(resId: Int): String = context.getString(resId)
}
