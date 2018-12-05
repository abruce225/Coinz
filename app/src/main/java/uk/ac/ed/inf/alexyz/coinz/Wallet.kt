package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_wallet.*
import org.jetbrains.anko.toast
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*

import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.activity_my_store.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList



@SuppressLint("MissingPermission", "SetTextI18n")
class Wallet : AppCompatActivity() {

    private lateinit var collectedCoins:ArrayList<Coin>

    private lateinit var depositBox:ArrayList<Coin>

    private lateinit var returnToWallet:ArrayList<Coin>

    private lateinit var alreadyCashed:ArrayList<Coin>

    private lateinit var mAuth: FirebaseAuth

    private lateinit var myDataBase: FirebaseDatabase

    private lateinit var mRootRef: DatabaseReference

    private lateinit var userName: String

    private lateinit var listView: ListView

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy/MM/dd")

    private val mContext = this

    private lateinit var todaydate: String

    private var goldSumInDeposit: Double = 0.0

    private var goldSum: Double = 0.0

    private var todayCoins: Int = 0

    private var quid: Float = 0.toFloat()
    private var dolr: Float = 0.toFloat()
    private var shil: Float = 0.toFloat()
    private var peny: Float = 0.toFloat()

    private var bankless =false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        depositBox = arrayListOf()
        collectedCoins = arrayListOf()
        alreadyCashed = arrayListOf()
        todaydate = sdf.format(Date())
        returnToWallet = arrayListOf()
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        val sharedPrefs = MySharedPrefs(this)
        if(sharedPrefs.getPP()){
            showInformationPopup()
        }
        mRootRef.child("users/$userName/bankless").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("can't access your data right now")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){
                    bankless = p0.value.toString() == todaydate
                }
            }
        })
        quid = sharedPrefs.getQUID()
        dolr = sharedPrefs.getDOLR()
        shil = sharedPrefs.getSHIL()
        peny = sharedPrefs.getPENY()
        setTextViews()
        mRootRef.child("users/$userName/netWorth").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    goldSum = p0.value.toString().toDouble()
                }
            }
        })
        mRootRef.child("users/$userName/coinsToday").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    todayCoins = p0.value.toString().toInt()
                    toast("You've currently cashed in $todayCoins coins today.")
                } else {
                    toast("You haven't banked any coins yet today!")
                }
            }
        })
        mRootRef.child("users/$userName/collectedCoins").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val myGSON = Gson()
                    val json = p0.value.toString()
                    if (json != "") {
                        val coinType = object : TypeToken<List<Coin>>() {}.type
                        collectedCoins = myGSON.fromJson(json, coinType)
                        setTextViews()
                        createListView()

                    }else{
                        setTextViews()
                        createListView()
                    }
                }else{
                    setTextViews()
                }
            }
        })
        mRootRef.child("days/${sdf.format(Date())}/alreadyCashedCoins").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val myGSON = Gson()
                    val json = p0.value.toString()
                    if (json != "") {
                        val coinType = object : TypeToken<List<Coin>>() {}.type
                        alreadyCashed = myGSON.fromJson(json, coinType)
                    }
                }
            }
        })

        buttonSyncSelected.setOnClickListener {
            if (depositBox.size > 0){
                val mll = LatLng(0.0,0.0)
                mll.latitude = sharedPrefs.getLAT().toDouble()
                mll.longitude = sharedPrefs.getLON().toDouble()
                if(LatLng(55.942963,-3.189014).distanceTo(mll) < 50 || bankless) {
                    cashInAllDeposit()
                    listView.getItemAtPosition(0)
                }else{
                    toast("You must be at the central bank to cash in coins!")
                }
            }else{
                toast("No coins in your deposit box!")
            }
        }
        buttonSubCons.setOnClickListener {
            openDepositPopup()
        }
        tapBarMenuWallet.setOnClickListener{
            tapBarMenuWallet.toggle()
        }
        goHomeWallet.setOnClickListener{
            val intent = Intent(this, CoinzHome::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        openPopupWallet.setOnClickListener{
            showInformationPopup()
        }
        openUserProfileWallet.setOnClickListener{
            startActivity(Intent(this,UserProfile::class.java))
        }
        displayRatesWallet.setOnClickListener{
            AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${sharedPrefs.getSHIL()}\nDOLR: ${sharedPrefs.getDOLR()}\nPENY: ${sharedPrefs.getPENY()}\nQUID: ${sharedPrefs.getQUID()}\n")
                show()
            }
        }
    }

    private fun openDepositPopup(){
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            returnSelectedToWallet()
            Toast.makeText(applicationContext, "Returned to your Wallet.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Your Deposit Box")
        val ids = Array(depositBox.size){"it = $it"}
        for ((count, a) in depositBox.withIndex()){
            ids[count] = a.id
        }
        builder.setMultiChoiceItems(ids, null) { _, which, isChecked ->
            if (isChecked) {
                returnToWallet.add(depositBox[which])
            } else if (returnToWallet.contains(depositBox[which])) {
                returnToWallet.remove(depositBox[which])
            }
        }
        builder.setPositiveButton("Return these coins to wallet." ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.create()
        builder.show()
    }

    private fun setTextViews(){
        textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
        buttonSubCons.text = "Deposit box:\n${depositBox.size} coins."
    }
    private fun returnSelectedToWallet(){
        for(a in returnToWallet){
            depositBox.remove(a)
            collectedCoins.add(a)
        }
        listView.getItemAtPosition(0)
        returnToWallet.clear()
        setTextViews()
    }

    private fun cashInAllDeposit(){
        var dps: Int = depositBox.size - 1
        if(dps == -1){
            toast("You must add coins to your deposit box before you can cash in!")
            return
        }
        var beatenTo = 0
        while(dps >= 0){
            for (b in alreadyCashed) {
                if (b.id == depositBox[dps].id) {
                    beatenTo++
                    depositBox.removeAt(dps)
                    break
                }
            }
            dps--
        }
        listView.getItemAtPosition(0)
        dps = depositBox.size
        if(beatenTo > 0){
            toast("Unfortunately, another player has already cashed $beatenTo of these Coins. I'll only cash non-duplicates.")
        }
        if(dps>0 && todayCoins+dps <= 25){
            val myGSON = Gson()
            val json = myGSON.toJson(collectedCoins)
            mRootRef.child("users").child(userName).child("collectedCoins").setValue(json)
            mRootRef.child("users").child(userName).child("coinsToday").setValue(todayCoins+dps)
            depositBox.addAll(alreadyCashed)
            mRootRef.child("days/${sdf.format(Date())}/alreadyCashedCoins").setValue(myGSON.toJson(depositBox))
            mRootRef.child("users").child(userName).child("netWorth").setValue(goldSum+goldSumInDeposit)
            depositBox.clear()
            setTextViews()
        }else if(dps == 0){
            val myGSON = Gson()
            val json = myGSON.toJson(collectedCoins)
            mRootRef.child("users").child(userName).child("collectedCoins").setValue(json)
            depositBox.clear()
            setTextViews()
            toast("Looks like someone beat you to all of your coins! Unlucky!")
        }else{
            toast("You're trying to cash in ${todayCoins+dps-25} too many coins, please remove some from your deposit box!")
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun createListView(){
        listView = findViewById(R.id.listViewWallet)
        listView.adapter = CoinAdapter(this, collectedCoins)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val x:Coin = collectedCoins[position]
            if(sdf.format(Date()) == x.date){
                collectedCoins.remove(x)
                depositBox.add(x)
                listView.getItemAtPosition(position)
                if(x.currency == "QUID"){
                    goldSumInDeposit += (x.value*quid)
                }
                if(x.currency == "DOLR"){
                    goldSumInDeposit += (x.value*dolr)
                }
                if(x.currency == "SHIL"){
                    goldSumInDeposit += (x.value*shil)
                }
                if(x.currency == "PENY"){
                    goldSumInDeposit += (x.value*peny)
                }
            }else{
                createExpiredPopup()

            }
            setTextViews()
        }
    }

    private fun createExpiredPopup(){
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            removeExpired()
            Toast.makeText(applicationContext, "Removed all expired coins.", Toast.LENGTH_SHORT).show()
        }
        val negativeButtonClick = { _: DialogInterface, _: Int ->
            Toast.makeText(applicationContext, "Expired coins still present in your wallet.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Coin Expired!")
        builder.setMessage("Unfortunately, this coin has expired. Would you like me to remove all expired coins from your wallet?")
        builder.setPositiveButton("Remove" ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Keep" ,DialogInterface.OnClickListener(function = negativeButtonClick))
        builder.create()
        builder.show()
    }

    private fun removeExpired(){
        for(coin in collectedCoins){
            if (sdf.format(Date()) != coin.date){
                collectedCoins.remove(coin)
            }
        }
        listView.getItemAtPosition(0)
    }
    private fun showInformationPopup(){
        val builder = AlertDialog.Builder(this)
        val positiveButtonClick = { _: DialogInterface, _: Int ->}
        builder.setTitle("Information for Wallet")
        builder.setMessage("This is your wallet. When you collect coins they end up here! You can hold as many coins as you like in your wallet, however coins expire at midnight so make sure" +
                " you cash them in before then." +
                "\n\nYou can cash in up to 25 coins every day, so if you collect more you'll either have to let them expire or trade them to a friend! To trade open your profile from the main menu, " +
                "or the button below.\n\nTo cash in coins you must be at the bank! It is located at the library, however you've got to be quick, as every coin may only be banked once per day. So" +
                " if another player gets there first, you won't get any gold!" +
                "\n\nTo remove coins from your deposit box, simply tap it!\n\nHave fun and collect fast!")
        builder.setPositiveButton("Got it!", DialogInterface.OnClickListener(positiveButtonClick))
        builder.create()
        builder.show()
    }
}
