package com.realityexpander.routes

import com.realityexpander.respondCss
import io.ktor.application.*
import io.ktor.routing.*
import kotlinx.css.*

fun Route.styleRoute() {
    route("/static/css/styles.css") {
        get {
            call.respondCss {
                body {
                    fontSize = 1.5.em
                    backgroundColor = Color.darkGray
                    display = Display.inlineBlock
                }
                p {
                    color = Color.green
                }
                h3 {
                    color = Color.red
                    backgroundColor = Color.blue
                    fontSize = 35.px
                }

                // Use a selector to style a specific element
                rule("div h2") {
                    backgroundColor = Color.yellow
                    color=Color.black
                }
            }
        }
    }
}