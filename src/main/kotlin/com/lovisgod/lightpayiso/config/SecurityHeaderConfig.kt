//package com.lovisgod.lightpayiso.config
//
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.security.config.Customizer
//import org.springframework.security.config.annotation.web.builders.HttpSecurity
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
//import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer
//import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig
//import org.springframework.security.web.SecurityFilterChain
//import org.springframework.security.web.util.matcher.AnyRequestMatcher
//
//
//@Configuration
//@EnableWebSecurity
//class WebSecurityConfig {
//
//    @Bean
//    @Throws(Exception::class)
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
//        http
//            .headers {
//
//                it.httpStrictTransportSecurity(Customizer {
//                  it.includeSubDomains(true).maxAgeInSeconds(31536000).requestMatcher(AnyRequestMatcher.INSTANCE)
//                })
//
//                it.frameOptions {
//                    it.sameOrigin()
//                }
//            }
//
//        return http.build()
//    }
//
//    @Bean
//    @Throws(java.lang.Exception::class)
//    fun filterChain(http: HttpSecurity): SecurityFilterChain {
//        http // ...
//            .headers { headers: HeadersConfigurer<HttpSecurity?> ->
//                headers
//                    .frameOptions(
//                        Customizer {
//                            it.sameOrigin()
//                        }
//                    )
//            }
//        return http.build()
//    }
//}