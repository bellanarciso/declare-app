package edu.illinois.cs.cs124.ay2026.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.illinois.cs.cs124.ay2026.project.ui.theme.Cream
import edu.illinois.cs.cs124.ay2026.project.ui.theme.DeclaredText
import edu.illinois.cs.cs124.ay2026.project.ui.theme.DeclaredTheme
import edu.illinois.cs.cs124.ay2026.project.ui.theme.Forest

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeclaredTheme(darkTheme = false) {
                WelcomeScreen(
                    onGetStarted = {
                        startActivity(Intent(this, OnboardingActivity::class.java))
                        finish()
                    },
                    onSignIn = {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "hello,",
            style = DeclaredText.script,
            color = Forest
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Let's plan\nyour four years.",
            style = MaterialTheme.typography.displayMedium,
            color = Forest,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Four-year planning, finally made simple.",
            style = DeclaredText.tagline,
            color = Forest.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(56.dp))

        Button(
            onClick = onGetStarted,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Forest,
                contentColor   = Cream
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Get started",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onSignIn) {
            Text(
                text = "I already have an account",
                style = MaterialTheme.typography.labelLarge,
                color = Forest,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun WelcomeScreenPreview() {
    DeclaredTheme(darkTheme = false) {
        WelcomeScreen(onGetStarted = {}, onSignIn = {})
    }
}