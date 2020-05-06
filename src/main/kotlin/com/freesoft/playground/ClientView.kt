package com.freesoft.playground

import javafx.beans.property.SimpleStringProperty
import javafx.scene.Parent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*

class ClientApp : App(ClientView::class)

class ClientView : View("Coroutine UI View") {

    private val finder: HttpWaldoFinder by inject()
    private val inputText = SimpleStringProperty("Jane")
    private val resultText = SimpleStringProperty("")

    override val root: Parent = form {
        fieldset("Lets find Waldo") {
            field("First Name:") {
                textfield().bind(inputText)
                button("Search") {
                    action {
                        println("Running event handler".withThreadId())
                        searchForWaldo()
                    }
                }
            }
            field("Result:") {
                label(resultText)
            }
        }
    }

    private fun searchForWaldo() {
        GlobalScope.launch(Dispatchers.Main) {
            println("Doing coroutines".withThreadId())
            val input = inputText.value
            val output = finder.wheresWaldo(input)
            resultText.value = output
        }
    }
}