package uk.ac.ed.inf.alexyz.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login_screen.*
import org.jetbrains.anko.toast

class LoginScreen : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var password: EditText
    private lateinit var email: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)
        email = findViewById(R.id.signup_email_input)
        password = findViewById(R.id.signup_password_input)
        mAuth = FirebaseAuth.getInstance()
        button_login.setOnClickListener{view->
            loginUser()
        }
        button_register.setOnClickListener{view ->
            registerUser()
        }
    }

    private fun registerUser(){
        val myEmail:String = email.text.toString().trim()
        val myPassword:String = password.text.toString().trim()

        if(TextUtils.isEmpty(myPassword)) {
            toast("You must enter a password to register.")
            return
        }

        if(TextUtils.isEmpty(myEmail)) {
            toast("You must enter an email to register.")
            return
        }

        mAuth.createUserWithEmailAndPassword(myEmail,myPassword).addOnCompleteListener{task ->
            if(task.isSuccessful){
                toast("Registration Successful\nPlease Log-in to play!")
            }else{
                toast("Registration Failed\nPlease try again.")
            }
        }

    }

    private fun loginUser(){
        val myEmail:String = email.text.toString().trim()
        val myPassword:String = password.text.toString().trim()

        if(TextUtils.isEmpty(myPassword)) {
            toast("You must enter a password to Log-in.")
            return
        }

        if(TextUtils.isEmpty(myEmail)) {
            toast("You must enter an email to Log-in.")
            return
        }

        mAuth.signInWithEmailAndPassword(myEmail,myPassword).addOnCompleteListener{task ->
            if(task.isSuccessful){
                toast("Log-in complete\nHave fun!")
                startActivity(Intent(this,CoinzHome::class.java))
            }else{
                toast("Log-in failed\nPlease check your details.")
            }
        }
    }

}