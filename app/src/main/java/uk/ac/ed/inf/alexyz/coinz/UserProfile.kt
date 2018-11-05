package uk.ac.ed.inf.alexyz.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_user_profile.*

class UserProfile : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        val mySharedPrefs:MySharedPrefs = MySharedPrefs(this)
        mAuth = FirebaseAuth.getInstance()
        tvName.setText("Logged in as: ${mAuth.currentUser?.email}")
        tvDescription.setText("Current NET Worth: ${mySharedPrefs.getGoldSum()}")
    }
}
