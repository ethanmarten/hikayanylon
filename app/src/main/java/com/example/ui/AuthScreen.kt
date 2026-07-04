package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.FirebaseMode
import com.example.firebase.FirebaseService
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MainViewModel, onDismiss: (() -> Unit)? = null) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Regular User") } // "Regular User" or "Admin"
    var passwordVisible by remember { mutableStateOf(false) }

    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavySurface)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Gold Crown Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(GoldPrimary, GoldDark)))
                    .shadow(16.dp)
                    .border(BorderStroke(1.5.dp, BorderGold), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Hikaya Logo",
                    tint = NavyDeep,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "بوابة حكاية الفاخرة",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (isLogin) "الولوج الآمن للعملاء والمدراء" else "إنشاء حساب فاخر جديد",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(24.dp))
                    .shadow(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Auth Tab Swapper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDeep, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        val activeRegBg = if (!isLogin) Modifier.background(Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))) else Modifier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(activeRegBg)
                                .clickable { isLogin = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تسجيل جديد",
                                fontWeight = FontWeight.Bold,
                                color = if (!isLogin) NavyDeep else TextMuted,
                                fontSize = 14.sp
                            )
                        }

                        val activeLogBg = if (isLogin) Modifier.background(Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))) else Modifier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(activeLogBg)
                                .clickable { isLogin = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تسجيل دخول",
                                fontWeight = FontWeight.Bold,
                                color = if (isLogin) NavyDeep else TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Alert Banner
                    authError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4A1515))
                                .border(BorderStroke(1.dp, Color(0xFFEF9A9A)), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF9A9A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = Color(0xFFEF9A9A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearAuthError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFEF9A9A), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Input Fields
                    if (!isLogin) {
                        // Name Input for registration
                        Text(
                            text = "الإسم الكريم:",
                            fontSize = 12.sp,
                            color = GoldAccent,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            textAlign = TextAlign.Right
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("أدخل إسمك الكامل...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, color = TextOnNavy),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderGold,
                                focusedContainerColor = NavyDeep,
                                unfocusedContainerColor = NavyDeep
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email Input
                    Text(
                        text = "البريد الإلكتروني:",
                        fontSize = 12.sp,
                        color = GoldAccent,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        textAlign = TextAlign.Right
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("example@hikaya.com", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("email_input"),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, color = TextOnNavy),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderGold,
                            focusedContainerColor = NavyDeep,
                            unfocusedContainerColor = NavyDeep
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldPrimary) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    Text(
                        text = "كلمة المرور:",
                        fontSize = 12.sp,
                        color = GoldAccent,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        textAlign = TextAlign.Right
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("أدخل كلمة المرور...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, color = TextOnNavy),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderGold,
                            focusedContainerColor = NavyDeep,
                            unfocusedContainerColor = NavyDeep
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "عرض كلمة المرور",
                                    tint = GoldPrimary
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Action Button
                    Button(
                        onClick = {
                            if (isLogin) {
                                viewModel.login(email, password)
                            } else {
                                viewModel.register(email, password, name, "Regular User")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_submit_button"),
                        enabled = !authLoading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(color = NavyDeep, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isLogin) "تسجيل الدخول الفاخر" else "تأكيد التسجيل والمتابعة",
                                color = NavyDeep,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (onDismiss != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الولوج كضيف (تصفح فقط) 👤",
                                color = GoldAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
