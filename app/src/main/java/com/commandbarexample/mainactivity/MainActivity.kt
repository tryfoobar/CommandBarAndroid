package com.commandbarexample.mainactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.commandbarexample.mainactivity.ui.theme.MainActivityTheme
import com.commandbar.android.CommandBar
import com.commandbar.android.CommandBarOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainActivityTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = {
                            CommandBar.openHelpHub( (this@MainActivity), CommandBarOptions("641ade4d"))
                      },
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                    ) {
                        Text(text = "Open HelpHub")
                    }
                }
            }
        }
    }
}
