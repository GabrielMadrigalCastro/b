package kode.backend.backendkode

import org.springframework.security.authentication.AuthenticationProvider


//import kode.backend.backendkode.AppCustomDsl.Companion.customDsl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import jakarta.annotation.Resource

@Profile("initlocal")
@Configuration
@EnableWebSecurity
class OpenSecurityConfiguration{

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf{
                it.disable()
            }
            .cors{
                it.disable()
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/api/ia/**").permitAll()
                    .requestMatchers("/v1/**").permitAll()
                    .anyRequest().authenticated()
            }

        return http.build()
    }

}

@Profile("!initlocal")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class JwtSecurityConfiguration {

    @Value("\${url.unsecure}")
    val urlUnsecure : String? = null

    @Value("\${url.users.signup}")
    val urlSignup: String? = null

    @Value("\${url.login}")
    val urlLogin: String? = null

    @Resource
    private val userDetailsService: AppUserDetailsService? = null

    @Bean
    @Throws(java.lang.Exception::class)
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager? {
        return authConfig.authenticationManager
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder? {
        return BCryptPasswordEncoder()
    }

    @Suppress("DEPRECATION")
    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Value("\${cors.allowed-origins:http://localhost:3000}")
    private val corsAllowedOrigins: String = "http://localhost:3000"

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf{
                it.disable()
            }
            .cors{
                it.configurationSource(corsConfigurationSource())
            }
            .authorizeHttpRequests {
                it
                    // El preflight de CORS nunca lleva token
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Únicos endpoints públicos: autenticarse y registrarse
                    .requestMatchers(HttpMethod.POST, urlLogin).permitAll()
                    .requestMatchers(HttpMethod.POST, urlSignup).permitAll()
                    .requestMatchers("/".plus(urlUnsecure).plus("/**")).permitAll()
                    // Todo lo demás exige un JWT válido
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                // Sin esto, una petición sin token responde 403 en vez de 401
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .sessionManagement{
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider())
            .with(AppCustomDsl(userDetailsService!!)) {}

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().apply {
            allowCredentials = true
            corsAllowedOrigins.split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach(::addAllowedOrigin)
            addAllowedHeader("*")
            addAllowedMethod("*")
            // El navegador oculta las cabeceras de respuesta por defecto: sin esto
            // el frontend no puede leer el token que devuelve el login.
            addExposedHeader(HttpHeaders.AUTHORIZATION)
        }
        source.registerCorsConfiguration("/**", config)
        return source
    }

}

class AppCustomDsl(
    private val userDetailsService: AppUserDetailsService
) : AbstractHttpConfigurer<AppCustomDsl?, HttpSecurity?>() {

    override fun configure(http: HttpSecurity?) {
        val security = http ?: return
        val authenticationManager = security.getSharedObject(AuthenticationManager::class.java)

        security.addFilter(JwtAuthenticationFilter(authenticationManager))
        security.addFilter(JwtAuthorizationFilter(authenticationManager, userDetailsService))
    }
}