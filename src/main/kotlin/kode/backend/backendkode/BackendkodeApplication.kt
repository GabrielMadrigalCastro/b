package kode.backend.backendkode

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class BackendkodeApplication

fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext = runApplication<BackendkodeApplication>(*args)

    // Listar todos los endpoints registrados
    println("========================================")
    println("REGISTERED ENDPOINTS:")
    println("========================================")

    val requestMappingHandlerMapping = context.getBean(RequestMappingHandlerMapping::class.java)
    val map = requestMappingHandlerMapping.handlerMethods

    map.forEach { (key, value) ->
        println("${key.patternsCondition} -> ${value.method.name}")
    }

    println("========================================")
}