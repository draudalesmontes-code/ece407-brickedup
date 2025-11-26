package com.cs407.brickcollector.ui

import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cs407.brickcollector.R
import com.cs407.brickcollector.models.UserDatabase
import com.cs407.brickcollector.models.User
import com.cs407.brickcollector.models.UserFirestore
import com.cs407.brickcollector.models.UserState
import kotlinx.coroutines.runBlocking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch

@Composable
fun ErrorText(error: String?, modifier: Modifier = Modifier) {
    if (error != null)
        Text(text = error, color = Color.Red, textAlign = TextAlign.Center)
}

@Composable
fun userEmail(modifier: Modifier = Modifier): String {
    var email by remember { mutableStateOf("") }

    TextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.email_hint)) })

    return email
}

@Composable
fun userPassword(modifier: Modifier = Modifier): String {
    var passwd by remember { mutableStateOf("") }

    TextField(
        value = passwd,
        onValueChange = { passwd = it },
        label = { Text(stringResource(R.string.password_hint)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )

    return passwd
}

fun createAccount(
    email: String,
    password: String,
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit,
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            onComplete(task.isSuccessful, task.exception, auth.currentUser)
        }
}

fun signIn(
    email: String,
    password: String,
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit,
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            val user = if (task.isSuccessful) auth.currentUser else null
            onComplete(task.isSuccessful, task.exception, user)
        }
}

//fun hash(input: String): String {
//    return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
//        .fold("") { str, it -> str + "%02x".format(it) }
//}

@Composable
fun LogInSignUpButton(
    email: String,
    password: String,
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
//    val userPasswdKV =
//        context.getSharedPreferences(context.getString(R.string.userPasswdKV), Context.MODE_PRIVATE)

    Button(onClick = {
        var errorString: String? = null

        val emailResult = checkEmail(email)
        if (emailResult == EmailResult.Empty) {
            errorString = context.getString(R.string.empty_email)
        } else if (emailResult == EmailResult.Invalid) {
            errorString = context.getString(R.string.invalid_email)
        }

        val passwordResult = checkPassword(password)
        if (errorString == null) {
            errorString = when (passwordResult) {
                PasswordResult.Empty -> {
                    context.getString(R.string.empty_password)
                }

                PasswordResult.Short -> {
                    context.getString(R.string.short_password)
                }

                PasswordResult.Invalid -> {
                    context.getString(R.string.invalid_password)
                }

                PasswordResult.Valid -> {
                    null
                }
            }
        }

        if (errorString != null)
            onComplete(false, Exception(errorString), null)
        else
            signIn(email, password, onComplete)
    }) {
        Text(stringResource(R.string.login_button))
    }
}

enum class EmailResult {
    Valid,
    Empty,
    Invalid,
}

fun checkEmail(email: String): EmailResult {
    if (email.isEmpty())
        return EmailResult.Empty
    // 1. username of email should only contain "0-9, a-z, _, A-Z, ."
    // 2. there is one and only one "@" between username and server address
    // 3. there are multiple domain names with at least one top-level domain
    // 4. domain name "0-9, a-z, -, A-Z" (could not have "_" but "-" is valid)
    // 5. multiple domain separate with '.'
    // 6. top level domain should only contain letters and at lest 2 letters
    // Remind students this email check only valid for this course
    val pattern = Regex("^[\\w.]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")
    return if (pattern.matches(email)) EmailResult.Valid else EmailResult.Invalid
}

enum class PasswordResult {
    Valid,
    Empty,
    Short,
    Invalid
}

fun checkPassword(password: String): PasswordResult {
    // 1. password should contain at least one uppercase letter, lowercase letter, one digit
    // 2. minimum length: 5
    if (password.isEmpty())
        return PasswordResult.Empty
    if (password.length < 5)
        return PasswordResult.Short
    if (Regex("\\d+").containsMatchIn(password) &&
        Regex("[a-z]+").containsMatchIn(password) &&
        Regex("[A-Z]+").containsMatchIn(password)
    )
        return PasswordResult.Valid
    return PasswordResult.Invalid
}

fun updateName(name: String, onComplete: (Boolean, Exception?) -> Unit) {
    val user = Firebase.auth.currentUser

    val profileUpdates = userProfileChangeRequest {
        displayName = name
//        photoUri = Uri.parse("https://example.com/jane-q-user/profile.jpg")
    }

    user!!.updateProfile(profileUpdates)
        .addOnCompleteListener { task ->
            onComplete(task.isSuccessful, task.exception)
        }
}

@Composable
fun AskNamePage(
    onComplete: (Boolean, Exception?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }

    TextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.name_hint)) })
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = {
        updateName(name, onComplete)
    }) {
        Text(stringResource(R.string.confirm_button))
    }
}

@Composable
fun LoginPage(
    modifier: Modifier = Modifier, loginButtonClick: (UserState) -> Unit
) {
    var currentPage by remember { mutableStateOf("login") } // "login" or "signup"
    val auth = Firebase.auth
    val context = LocalContext.current
    val db = UserDatabase.getDatabase(context)

    // Automatically log in if user is already signed in
    LaunchedEffect(auth.currentUser) {
        val signedInUser = auth.currentUser
        if (signedInUser != null && !signedInUser.displayName.isNullOrEmpty()) {
            var user = db.userDao().getByUID(signedInUser.uid)
            if (user == null) {
                db.userDao().insert(User(userUID = signedInUser.uid, username = signedInUser.displayName!!))
                user = db.userDao().getByUID(signedInUser.uid)
            }
            loginButtonClick(UserState(user!!.userId, user.username, user.userUID))
        }
    }
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentPage == "login") {
                LoginScreen(
                    onLoginSuccess = loginButtonClick,
                    onNavigateToSignUp = { currentPage = "signup" }
                )
            } else {
                SignUpScreen(
                    onSignUpSuccess = loginButtonClick,
                    onNavigateToLogin = { currentPage = "login" }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen (
    onSignUpSuccess: (UserState) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val userFirestore = remember { UserFirestore() }

    val context = LocalContext.current
    val db = UserDatabase.getDatabase(context)
    val coroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Create Account", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        ErrorText(error)

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { stringResource(R.string.signup_email) })
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { stringResource(R.string.signup_name) })
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { stringResource(R.string.signup_city) })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { R.string.signup_password },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { R.string.signup_confirm_password },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Button(
            onClick = {
                // Validation logic
                if (email.isBlank() || name.isBlank() ||  password.isBlank()) {
                    error = "Please fill all required fields."
                    return@Button
                }
                if (password != confirmPassword) {
                    error = "Passwords do not match."
                    return@Button
                }
                isLoading = true
                createAccount(email, password) { isSuccess, exception, firebaseUser ->
                    if (isSuccess && firebaseUser != null) {
                        updateName(name) { nameSuccess, nameException ->
                            if (nameSuccess) {
                                // store user's name and city in firestore
                                userFirestore.saveUserToFireStore(
                                    user = firebaseUser,
                                    name = name,
                                    city = city.ifBlank { null }
                                ) { firestoreSuccess, firestoreException ->
                                    if (firestoreSuccess) {
                                        coroutineScope.launch {
                                            // Storing the new user in Room DB
                                            db.userDao().insert(User(userUID = firebaseUser.uid, username = name, city = city.ifBlank{null}))
                                            val user = db.userDao().getByUID(firebaseUser.uid)
                                            onSignUpSuccess(UserState(user!!.userId, name, firebaseUser.uid))
                                        }
                                    } else {
                                        error = firestoreException?.message
                                        isLoading = false
                                    }
                                }

                            } else {
                                error = nameException?.message ?: "Failed to set user name."
                                isLoading = false
                            }
                        }
                    } else {
                        error = exception?.message ?: "Sign up failed."
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.signup_button))
        }

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.to_login_button))
        }
    }
}

@Composable
fun LoginScreen (
    onLoginSuccess: (UserState) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val db = UserDatabase.getDatabase(LocalContext.current)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Welcome Back", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        ErrorText(error)

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Button(
            onClick = {
                var errorString: String? = null
                val passwordResult = checkPassword(password)
                if (errorString == null) {
                    if (passwordResult == PasswordResult.Empty) {
                        errorString = context.getString(R.string.empty_password)
                    }
                }
                isLoading = true
                signIn(email, password) { isSuccess, exception, firebaseUser ->
                    if (isSuccess && firebaseUser != null && !firebaseUser.displayName.isNullOrEmpty()) {
                        coroutineScope.launch {
                            var user = db.userDao().getByUID(firebaseUser.uid)
                            if (user == null) {
                                db.userDao().insert(User(userUID = firebaseUser.uid, username = firebaseUser.displayName!!))
                                user = db.userDao().getByUID(firebaseUser.uid)
                            }
                            onLoginSuccess(UserState(user!!.userId, firebaseUser.displayName!!, firebaseUser.uid))
                        }
                    } else {
                        error = exception?.message ?: "Login failed. Please check your credentials."
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.login_button))
        }

        TextButton(onClick = onNavigateToSignUp) {
            Text(stringResource(R.string.to_signup_button))
        }
    }
}