package com.simpad.kneeboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simpad.kneeboard.data.api.ConnectionStatus
import com.simpad.kneeboard.ui.theme.LocalSimPadColors

@Composable
fun ConnectionDialog(
    initialHost: String,
    initialPort: Int,
    connectionStatus: ConnectionStatus,
    onDismiss: () -> Unit,
    onConnect: (host: String, port: Int) -> Unit,
    onDisconnect: () -> Unit
) {
    val colors = LocalSimPadColors.current
    var host by remember { mutableStateOf(initialHost) }
    var portText by remember { mutableStateOf(initialPort.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SimPad Server Connection",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your PC local Wi-Fi / LAN IP address running the SimPad Kneeboard Server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text("PC IP Address", color = colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() } },
                    label = { Text("HTTP & WebSocket Port", color = colors.textSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 8090
                    onConnect(host, port)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                )
            ) {
                Text("Connect", style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    TextButton(
                        onClick = {
                            onDisconnect()
                            onDismiss()
                        }
                    ) {
                        Text("Disconnect", color = colors.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = colors.textSecondary)
                }
            }
        },
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}
