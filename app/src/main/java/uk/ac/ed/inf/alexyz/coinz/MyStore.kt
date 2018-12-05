package uk.ac.ed.inf.alexyz.coinz

import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_map_box_main.*
import kotlinx.android.synthetic.main.activity_my_store.*
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*

class MyStore : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var myDataBase: FirebaseDatabase

    private lateinit var mRootRef: DatabaseReference

    private lateinit var userName: String

    private val sdf = SimpleDateFormat("yyyy/MM/dd")
    private var netWorth = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
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

        tapBarMenuStore.setOnClickListener{
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
            applyHoover()
        }
        nobankactivate.setOnClickListener{
            applyBankless()
        }
    }
    private fun setTexts(){
        currentWorth.text = "You currently have $netWorth in your account!\nSpend it wisely."
    }

    private fun applyHoover(){
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            if(netWorth >= 300) {
                mRootRef.child("users/${userName}/netWorth").setValue(netWorth - 300)
                mRootRef.child("users/${userName}/hoover").setValue(sdf.format(Date()))
                Toast.makeText(applicationContext, "Hoover activated.", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(applicationContext, "You're too poor.", Toast.LENGTH_SHORT).show()
            }
        }
        val negativeButtonClick = { _: DialogInterface, _: Int ->
            Toast.makeText(applicationContext, "Transaction aborted.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activate Hoover?")
        builder.setMessage("This will cost you 300 Gold and be active until midnight.")
        builder.setPositiveButton("Cancel" ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Activate" ,DialogInterface.OnClickListener(function = negativeButtonClick))
        builder.create()
        builder.show()
    }
    private fun applyBankless(){
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            if(netWorth >=500) {
                mRootRef.child("users/${userName}/netWorth").setValue(netWorth - 500)
                mRootRef.child("users/${userName}/bankless").setValue(sdf.format(Date()))
                Toast.makeText(applicationContext, "Bankless activated.", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(applicationContext, "You're too poor.", Toast.LENGTH_SHORT).show()
            }
        }
        val negativeButtonClick = { _: DialogInterface, _: Int ->
            Toast.makeText(applicationContext, "Transaction aborted.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activate Bankless Cash-in?")
        builder.setMessage("This will cost you 500 gold and be active until midnight.")
        builder.setPositiveButton("Cancel" ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Activate" ,DialogInterface.OnClickListener(function = negativeButtonClick))
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
