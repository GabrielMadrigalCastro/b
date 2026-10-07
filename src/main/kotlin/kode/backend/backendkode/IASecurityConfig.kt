package kode.backend.backendkode

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.context.annotation.Bean

@Configuration
@EnableWebSecurity
@Order(1) // Se ejecuta ANTES que JwtSecurityConfiguration
class IASecurityConfig {

    @Bean
    fun iaSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/ia/**","/api/ia/**") // Solo aplica a /api/ia/**
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.anyRequest().permitAll() // Permite TODO en /api/ia/**
            }

        return http.build()
    }
}

/**
 * Filtro alternativo si @Order no funciona
 * Permite bypass de seguridad para rutas de IA
 */
@Component
class IASecurityBypassFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        // Si la ruta es de IA, marcar como permitida
        if (path.startsWith("/api/ia")) {
            request.setAttribute("BYPASS_SECURITY", true)
        }

        filterChain.doFilter(request, response)
    }
}