package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_my_store.*
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*

class MyStore : AppCompatActivity() { //activity allowing users to buy powerups for gold

    private lateinit var mAuth: FirebaseAuth

    private lateinit var myDataBase: FirebaseDatabase

    private lateinit var mRootRef: DatabaseReference

    private lateinit var userName: String

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy/MM/dd")

    private var netWorth = 0.0

    private var currentBankless = ""

    private var currenthoover = ""

    override fun onCreate(savedInstanceState: Bundle?) { //very similar to set up for wallet and trading screen, initialise all vars and then download values to fill them.
        super.onCreate(savedInstanceState)
        val sharedPrefs = MySharedPrefs(this)
        setContentView(R.layout.activity_my_store)
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        if(sharedPrefs.getPP()){
            showInformationPopup()
        }
        mRootRef.child("users/$userName/netWorth").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    netWorth = p0.value.toString().toDouble()
                    setTexts()
                }else{
                    netWorth = 0.0
                    mRootRef.child("users/$userName/netWorth").setValue(0.0)
                    setTexts()
                }
            }
        })
        mRootRef.child("users/$userName/bankless").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    currentBankless = p0.value.toString()
                    if(currentBankless == sdf.format(Date())){ //disable button if user has already bought the powerup today
                        nobankactivate.isEnabled = false
                        nobankactivate.text = getString(R.string.activated)
                    }
                }
            }
        })
        mRootRef.child("users/$userName/hoover").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    currenthoover = p0.value.toString()
                    if(currenthoover == sdf.format(Date())){
                        hooveractivate.isEnabled = false
                        hooveractivate.text = getString(R.string.activated)
                    }
                }
            }
        })

        tapBarMenuStore.setOnClickListener{//tapbarmenu consistent with other activities.
            tapBarMenuStore.toggle()
        }
        openWalletStore.setOnClickListener{
            startActivity(Intent(this, Wallet::class.java))
        }
        openPopupStore.setOnClickListener{
            showInformationPopup()
        }
        openUserProfileStore.setOnClickListener{
            startActivity(Intent(this,UserProfile::class.java))
        }
        displayRatesStore.setOnClickListener{
            AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${sharedPrefs.getSHIL()}\nDOLR: ${sharedPrefs.getDOLR()}\nPENY: ${sharedPrefs.getPENY()}\nQUID: ${sharedPrefs.getQUID()}\n")
                show()
            }
        }
        hooveractivate.setOnClickListener{
            if(currentBankless == sdf.format(Date())){
                toast("You have already activated this today!")
            }else {
                applyHoover()
            }
        }
        nobankactivate.setOnClickListener{
            if(currentBankless == sdf.format(Date())){
                toast("You have already activated this today!")
            }else {
                applyBankless()
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setTexts(){
        currentWorth.text = "You currently have ${netWorth.toInt()} in your account!\nSpend it wisely."
    }

    private fun applyHoover(){
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            if(netWorth >= 1000) { //make sure the user has enough money
                mRootRef.child("users/$userName/netWorth").setValue(netWorth - 1000) //bill them 1000
                mRootRef.child("users/$userName/hoover").setValue(sdf.format(Date())) //update the database to show that they have an active hoover today. using date creates simple unique identifier
                Toast.makeText(applicationContext, "Hoover activated.", Toast.LENGTH_SHORT).show()
                setTexts()
            }else{
                Toast.makeText(applicationContext, "You're broke.", Toast.LENGTH_SHORT).show()
            }
        }
        val negativeButtonClick = { _: DialogInterface, _: Int ->
            Toast.makeText(applicationContext, "Transaction aborted.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activate Hoover?")
        builder.setMessage("This will cost you 1000 Gold and be active until midnight.")
        builder.setPositiveButton("Activate" ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Cancel" ,DialogInterface.OnClickListener(function = negativeButtonClick))
        builder.create()
        builder.show()
    }
    private fun applyBankless(){ //we basically just need to create a popup where the user can confirm or reject the powerup. This applies for the above aswell.
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            if(netWorth >=1500) {
                mRootRef.child("users/$userName/netWorth").setValue(netWorth - 1500)
                mRootRef.child("users/$userName/bankless").setValue(sdf.format(Date()))
                Toast.makeText(applicationContext, "Bankless activated.", Toast.LENGTH_SHORT).show()
                setTexts()
            }else{
                Toast.makeText(applicationContext, "You're broke.", Toast.LENGTH_SHORT).show()
            }
        }
        val negativeButtonClick = { _: DialogInterface, _: Int ->
            Toast.makeText(applicationContext, "Transaction aborted.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activate Bankless Cash-in?")
        builder.setMessage("This will cost you 1500 gold and be active until midnight.")
        builder.setPositiveButton("Activate" ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Cancel" ,DialogInterface.OnClickListener(function = negativeButtonClick))
        builder.create()
        builder.show()
    }

    private fun showInformationPopup(){
        val builder = AlertDialog.Builder(this)
        val positiveButtonClick = { _: DialogInterface, _: Int -> }
        builder.setTitle("Information for Shop Activity")
        builder.setMessage("Welcome to the secret shop! In here you can purchase power ups to augment your Coinz experience.\n" +
                "\nSick of going to the bank to cash in? Turn on a power-up and you can cash in from home!\n" +
                "\nCan't get close enough to that 10 SHIL value coin, stuck in the middle of McEwan hall?? Turn on the hoover and you'll be able to pick it up from the middle of Bristo square!")
        builder.setPositiveButton("Got it!", DialogInterface.OnClickListener(positiveButtonClick))
        builder.create()
        builder.show()
    }
}
