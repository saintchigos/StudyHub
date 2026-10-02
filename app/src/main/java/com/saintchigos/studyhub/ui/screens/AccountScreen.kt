package com.saintchigos.studyhub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.saintchigos.studyhub.ui.AuthResult
import com.saintchigos.studyhub.ui.CommunityViewModel
import com.saintchigos.studyhub.ui.components.ScreenHeader
import com.saintchigos.studyhub.util.Security

private enum class AuthMode { SIGN_IN, REGISTER, VERIFY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit
) {
    val account by viewModel.account.collectAsState()
    val pendingUser by viewModel.needsVerification.collectAsState()
    val issuedCode by viewModel.lastIssuedCode.collectAsState()

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    if (pendingUser != null) mode = AuthMode.VERIFY

    fun handle(res: AuthResult) {
        busy = false
        when (res) {
            is AuthResult.Success -> {
                viewModel.clearIssuedCode()
                onBack()
            }
            is AuthResult.NeedsVerification -> {
                username = res.username
                mode = AuthMode.VERIFY
            }
            is AuthResult.Error -> error = res.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            ScreenHeader(
                title = when (mode) {
                    AuthMode.SIGN_IN -> "Sign in"
                    AuthMode.REGISTER -> "Create your account"
                    AuthMode.VERIFY -> "Verify your account"
                },
                subtitle = when (mode) {
                    AuthMode.SIGN_IN -> "Use your username and password."
                    AuthMode.REGISTER -> "Pick a username others will know you by."
                    AuthMode.VERIFY -> "Enter the 6 digit code."
                }
            )

            if (account != null) {
                Text("Signed in as @${account?.username}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.signOut() }) { Text("Sign out") }
            }

            if (mode != AuthMode.VERIFY) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            if (mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name classmates see (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            if (mode != AuthMode.VERIFY) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }

            if (mode == AuthMode.VERIFY) {
                if (issuedCode != null) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Your code", style = MaterialTheme.typography.labelLarge)
                            Text(issuedCode!!, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.clearIssuedCode() }) { Text("Hide code") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("6 digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    busy = true
                    error = null
                    when (mode) {
                        AuthMode.SIGN_IN -> viewModel.signIn(username, password) { handle(it) }
                        AuthMode.REGISTER -> viewModel.register(username, displayName, password) { handle(it) }
                        AuthMode.VERIFY -> viewModel.confirmCode(username, password) { handle(it) }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else {
                    Text(when (mode) { AuthMode.SIGN_IN -> "Sign in"; AuthMode.REGISTER -> "Create account"; AuthMode.VERIFY -> "Confirm code" })
                }
            }

            Spacer(Modifier.height(8.dp))
            if (mode == AuthMode.VERIFY) {
                TextButton(onClick = { viewModel.requestNewCode(username) { handle(it) } }) { Text("Send a new code") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { mode = if (mode == AuthMode.SIGN_IN) AuthMode.REGISTER else AuthMode.SIGN_IN; error = null }) {
                        Text(if (mode == AuthMode.SIGN_IN) "Create an account" else "I already have an account")
                    }
                    TextButton(onClick = onBack) { Text("Not now") }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PasswordStrengthMeter(password: String) {
    if (password.isEmpty()) return
    val strength = Security.passwordStrength(password)
    val (fraction, label) = when (strength) {
        Security.PasswordStrength.TOO_SHORT -> 0.1f to "Too short"
        Security.PasswordStrength.WEAK -> 0.35f to "Too easy"
        Security.PasswordStrength.FAIR -> 0.65f to "Fair"
        Security.PasswordStrength.STRONG -> 1f to "Strong"
    }
    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    Text(label, style = MaterialTheme.typography.bodySmall)
}