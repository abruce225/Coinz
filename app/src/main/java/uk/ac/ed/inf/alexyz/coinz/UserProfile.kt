package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_user_profile.*
import org.jetbrains.anko.toast

class UserProfile : AppCompatActivity() { //simple activity which allows the user to view info on their account.
                                          //pulls info from the database and displays it
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mRootRef: DatabaseReference
    private lateinit var userName: String
    private lateinit var myDataBase: FirebaseDatabase
    private var goldSum: Double = 0.0
    private var coinsToday = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        mRootRef.child("users/$userName/netWorth").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    goldSum = p0.value.toString().toDouble()
                    setTexts()
                }else{
                    goldSum = 0.0
                    mRootRef.child("users/$userName/netWorth").setValue(0.0)
                }
            }
        })
        mRootRef.child("users/$userName/coinsToday").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    coinsToday = p0.value.toString().toInt()
                    setTexts()
                }else{
                    mRootRef.child("users/$userName/coinsToday").setValue(0)
                    setTexts()
                }
            }
        })
        mRootRef.child("trading/${(mAuth.currentUser?.uid ?:"").take(8)}").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (!(p0.exists())){
                    mRootRef.child("trading/${(mAuth.currentUser?.uid ?:"").take(8)}").setValue(mAuth.currentUser!!.uid)
                }
            }
        })
        launchMultiPlayer.setOnClickListener{
            startActivity(Intent(this, TradingScreen::class.java))
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setTexts(){
        tvName.text = "Logged in as: ${mAuth.currentUser?.email ?:""}\n\nYour unique trading ID is:\n${(mAuth.currentUser?.uid ?:"").take(8)}"
        tvDescription.text = "Current NET Worth: ${goldSum.toInt()}\n\nCoins cashed in today: $coinsToday"
    }
}
