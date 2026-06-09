package com.commandbarexample.mainactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commandbarexample.mainactivity.ui.theme.MainActivityTheme
import com.commandbar.android.CommandBar
import com.commandbar.android.CommandBarOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CommandBar.boot(CommandBarOptions(apiKey = "c4328322e952bc44741e09a9adf622e0"))

        setContent {
            MainActivityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(
                            onClick = {
                                CommandBar.openResourceCenter(
                                    this@MainActivity,
                                    onFallbackAction = {
                                        println("Received fallback action")
                                        CommandBar.closeResourceCenter()
                                    },
                                )
                            },
                        ) {
                            Text(text = "Open Resource Center")
                        }
                        Button(
                            onClick = {
                                CommandBar.openAssistant(
                                    this@MainActivity,
                                    onFallbackAction = {
                                        println("Received fallback action")
                                        CommandBar.closeResourceCenter()
                                    },
                                )
                            },
                        ) {
                            Text(text = "Open Assistant")
                        }
                    }
                }
            }
        }
    }
}
