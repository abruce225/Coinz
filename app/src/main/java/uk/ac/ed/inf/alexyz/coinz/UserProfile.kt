package uk.ac.ed.inf.alexyz.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_user_profile.*
import org.jetbrains.anko.toast

class UserProfile : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mRootRef: DatabaseReference
    private lateinit var userName: String
    private var goldSum: Float = 0.toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        mAuth = FirebaseAuth.getInstance()
        val mypref = MySharedPrefs(this)
        tvName.setText("Logged in as: ${mAuth.currentUser?.email}")
        tvDescription.setText("Current NET Worth: ${mypref.getGoldSum()}")
    }
}
