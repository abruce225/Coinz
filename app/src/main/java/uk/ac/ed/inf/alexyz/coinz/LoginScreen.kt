package uk.ac.ed.inf.alexyz.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login_screen.*
import org.jetbrains.anko.toast

class LoginScreen : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var password: EditText
    private lateinit var email: EditText
    private lateinit var myDataBase: FirebaseDatabase
    private lateinit var userName: String
    private lateinit var mRootRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)
        val mySharedPrefs = MySharedPrefs(this)
        email = findViewById(R.id.signup_email_input) //initialise our lateinit vars for Firebase authentication, along with our input views
        password = findViewById(R.id.signup_password_input)
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        if(mySharedPrefs.getEmail() != "") { //if the user has elected to store past info in shared prefs, we display it in the editTexts and toggle the switches on
            email.setText(mySharedPrefs.getEmail())
            switchemail.toggle()
        }
        if(mySharedPrefs.getPassword() != ""){
            password.setText(mySharedPrefs.getPassword())
            switchpassword.toggle()
        }
        button_login.setOnClickListener{
            loginUser()
        }
        button_register.setOnClickListener{
            registerUser()
        }
    }

    private fun registerUser(){
        val myEmail:String = email.text.toString().trim()
        val myPassword:String = password.text.toString().trim()//make sure both boxes are filled in, otherwise toast what the user must do to continue

        if(TextUtils.isEmpty(myPassword)) {
            toast("You must enter a password to register.")
            return
        }

        if(TextUtils.isEmpty(myEmail)) {
            toast("You must enter an email to register.")
            return
        }

        mAuth.createUserWithEmailAndPassword(myEmail,myPassword).addOnCompleteListener{task -> //register the new user with firebase
            if(task.isSuccessful){
                loginUser() //if successful, log them in
            }else{
                toast("Registration Failed\nPlease try again.")
            }
        }

    }

    private fun loginUser(){
        val mySharedPrefs = MySharedPrefs(this) //set up shared prefs, for storage of password and email. This is obv v insecure, but this is a prototype
        val myEmail:String = email.text.toString().trim()
        val myPassword:String = password.text.toString().trim()

        if(TextUtils.isEmpty(myPassword)) { //same as above, make sure that the user has put in some input
            toast("You must enter a password to Log-in.")
            return
        }

        if(TextUtils.isEmpty(myEmail)) {
            toast("You must enter an email to Log-in.")
            return
        }

        mAuth.signInWithEmailAndPassword(myEmail,myPassword).addOnCompleteListener{task -> //log in the user
            if(task.isSuccessful){ //if successful, we need to know if the user wanted their details saved. Use the toggle buttons to determine this.
                if (switchemail.isChecked) {
                    mySharedPrefs.setEmail(myEmail)
                }else{
                    mySharedPrefs.setEmail("")
                }
                if (switchpassword.isChecked) {
                    mySharedPrefs.setPassword(myPassword)
                }else{
                    mySharedPrefs.setPassword("")
                }
                toast("Log-in complete\nHave fun!")
                startActivity(Intent(this,CoinzHome::class.java)) //take them to main activity if login was successful
            }else{
                toast("Log-in failed\nPlease check your details.") //otherwise, inform them of the failure and let them try again.
            }
        }
    }

}