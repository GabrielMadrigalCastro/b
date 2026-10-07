package kode.backend.backendkode

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import java.security.Key
import java.util.*

object SecurityConstants {
    const val TOKEN_TYPE = "JWT"
    const val TOKEN_ISSUER = "secure-api"
    const val TOKEN_AUDIENCE = "secure-app"
    const val TOKEN_LIFETIME: Long = 864000000
    const val TOKEN_PREFIX = "Bearer "
    const val APPLICATION_JSON = "application/json"
    const val UTF_8 = "UTF-8"
    const val TOKEN_SECRET: String =
        "TUlfQ0xBVkVfU0VDUkVUQV9TRUdVUkFfU1VQRVJfTEFSR0FfWV9BTEVBVE9SSUFfMTIzNDU2Nzg5MF9BQkNERUY="
}


/**
 *
 */
class JwtAuthenticationFilter(authenticationManager: AuthenticationManager) : UsernamePasswordAuthenticationFilter() {

    private val authManager: AuthenticationManager

    init {
        setFilterProcessesUrl("/v1/users/login")
        authManager = authenticationManager
    }

    @Throws(AuthenticationException::class)
    override fun attemptAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): Authentication {

        if (request.method != "POST") {
            throw AuthenticationServiceException("Authentication method not supported: $request.method")
        }

        return try {
            val userLoginInput: UserLoginInput = ObjectMapper()
                .readValue(request.inputStream, UserLoginInput::class.java)
            authManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    userLoginInput.email,
                    userLoginInput.password,
                    ArrayList()
                )
            )
        } catch (exception: IOException) {
            throw RuntimeException(exception)
        }
    }

    override fun successfulAuthentication(
        request: HttpServletRequest, response: HttpServletResponse,
        filterChain: FilterChain, authentication: Authentication,
    ) {

        val objectMapper = ObjectMapper()
            .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val token = Jwts.builder()
            .signWith(key(), SignatureAlgorithm.HS512)
            .setHeaderParam("typ", SecurityConstants.TOKEN_TYPE)
            .setIssuer(SecurityConstants.TOKEN_ISSUER)
            .setAudience(SecurityConstants.TOKEN_AUDIENCE)
            .setSubject((authentication.principal as org.springframework.security.core.userdetails.User).username)
            .setExpiration(Date(System.currentTimeMillis() + SecurityConstants.TOKEN_LIFETIME))
            .compact()

        response.addHeader(HttpHeaders.AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + token)
        val out = response.writer
        response.contentType = SecurityConstants.APPLICATION_JSON
        response.characterEncoding = SecurityConstants.UTF_8
        val userService = SpringContext.getBean(AppUserDetailsService::class.java)
        val userDetails = userService.getUserDetails((authentication.principal as org.springframework.security.core.userdetails.User).username)

        out.print(objectMapper.writeValueAsString(userDetails))

        out.flush()
    }
}

/**
 * This function will return the key to sign the token
 */
private fun key(): Key {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SecurityConstants.TOKEN_SECRET))
}

/**
 * This class will validate the token
 */
class JwtAuthorizationFilter(
    authenticationManager: AuthenticationManager,
    private val userDetailsService: AppUserDetailsService,
) : BasicAuthenticationFilter(authenticationManager) {

    @Throws(IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse,
        filterChain: FilterChain,
    ) {

        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

        // Sin cabecera Authorization seguimos como anónimos: la cadena de seguridad
        // decidirá si la ruta es pública o responde 401.
        if (authorizationHeader == null || !authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authorizationHeader.substring(SecurityConstants.TOKEN_PREFIX.length)

        val username: String? = try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).body.subject
        } catch (exception: JwtException) {
            null            // firma inválida, token manipulado o caducado
        } catch (exception: IllegalArgumentException) {
            null            // token vacío o mal formado
        }

        if (username == null) {
            rejectUnauthorized(response, "Token inválido o expirado")
            return
        }

        // Releemos el usuario en cada petición para que un token de un usuario
        // borrado o deshabilitado deje de servir, y para cargar sus autoridades.
        val userDetails = try {
            userDetailsService.loadUserByUsername(username)
        } catch (exception: UsernameNotFoundException) {
            null
        }

        if (userDetails == null || !userDetails.isEnabled) {
            rejectUnauthorized(response, "Usuario no válido o deshabilitado")
            return
        }

        LoggedUser.logIn(username)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(username, null, userDetails.authorities)

        try {
            filterChain.doFilter(request, response)
        } finally {
            // El hilo se reutiliza entre peticiones: hay que limpiar siempre.
            LoggedUser.logOut()
            SecurityContextHolder.clearContext()
        }
    }

    private fun rejectUnauthorized(response: HttpServletResponse, message: String) {
        SecurityContextHolder.clearContext()
        LoggedUser.logOut()
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message)
    }

}

/**
 * Object to holder the user information
 */
object LoggedUser {
    private val userHolder = ThreadLocal<String>()
    fun logIn(user: String) {
        userHolder.set(user)
    }

    fun logOut() {
        userHolder.remove()
    }

    fun get(): String {
        return userHolder.get()
    }
}








