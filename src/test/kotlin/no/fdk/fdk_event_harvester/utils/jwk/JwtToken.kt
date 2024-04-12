package no.fdk.fdk_event_harvester.utils.jwk

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.*


class JwtToken (private val access: Access) {
    private val exp = Date().time + 120 * 1000
    private val aud = listOf("fdk-harvest-admin")

    private fun buildToken() : String{
        val claimset = JWTClaimsSet.Builder()
            .audience(aud)
            .expirationTime(Date(exp))
            .claim("user_name","1924782563")
            .claim("name", "TEST USER")
            .claim("given_name", "TEST")
            .claim("family_name", "USER")
            .claim("iss", "http://localhost:5050/auth/realms/fdk")
            .claim("authorities", access.authorities)
            .build()

        val signed = SignedJWT(JwkStore.jwtHeader(), claimset)
        signed.sign(JwkStore.signer())

        return signed.serialize()
    }

    override fun toString(): String {
        return buildToken()
    }

}

enum class Access(val authorities: String) {
    ORG_WRITE("organization:333222111:admin"),
    ROOT("system:root:admin")
}
