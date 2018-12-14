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



@SuppressLint("MissingPermission", "SetTextI18n") //very similar to Trading Screen, except implements no duplication rule and allows cashing in of multiple coins at once.
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

    val coinType = object : TypeToken<List<Coin>>() {}.type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        depositBox = arrayListOf() //initialise arrays to avoid null errors
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
        mRootRef.child("users/$userName/bankless").addValueEventListener(object: ValueEventListener{ //check if user has an active bankless powerup from the shop
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
                    if (json != "") { //check if there are any coins stored for user in firebase
                        collectedCoins = myGSON.fromJson(json, coinType)
                        setTextViews()
                        if(::listView.isInitialized) { //if the listview is already up and the dataset changes, we simply update the for the listview dataset
                            listView.getItemAtPosition(0)
                        }else{
                            createListView() //otherwise we must initialise and draw the listview with the new dataset
                        }
                    }else{ //no need to parse an empty JSON
                        collectedCoins = arrayListOf()
                        setTextViews()
                        if(::listView.isInitialized) {
                            listView.getItemAtPosition(0)
                        }else{
                            createListView()
                        }
                    }
                }else{
                    createListView()
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
                mll.longitude = sharedPrefs.getLON().toDouble() //pull the most recent location from shared prefs. This is updated every time onlocationchanged is called in mapBoxMain
                if(LatLng(55.942963,-3.189014).distanceTo(mll) < 50 || bankless) {//if user is at the bank or has a powerup cash in the coins
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
            if(depositBox.size > 0){
                openDepositPopup()
            }else{
                toast("You need to add some coins first!")
            }
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
        val positiveButtonClick = { _: DialogInterface, _: Int -> //call a function that returns all coins selected into collected coins
            returnSelectedToWallet()
            Toast.makeText(applicationContext, "Returned to your Wallet.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Your Deposit Box")
        val ids = Array(depositBox.size){"it = $it"}
        for ((count, a) in depositBox.withIndex()){ //create entry in popup for each coin in deposit box, displaying coin id
            ids[count] = a.id
        }
        builder.setMultiChoiceItems(ids, null) { _, which, isChecked ->
            if (isChecked) {
                returnToWallet.add(depositBox[which]) //every selected item should be removed from deposit box list and added to collectedCoins
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
    private fun returnSelectedToWallet(){ //simple iterator to move coins over from returntoWallet<Coin> to collectedCoins
        for(x in returnToWallet){
            depositBox.remove(x)
            collectedCoins.add(x)
        }
        listView.getItemAtPosition(0) //update the listview
        returnToWallet.clear()
        setTextViews()
    }

    private fun cashInAllDeposit(){ //funtion that iterates over all entries in deposit box and converts them to gold. Removes them from users account
        var dps: Int = depositBox.size - 1
        if(dps == -1){ //make sure that there is atleast one coin in deposit box
            toast("You must add coins to your deposit box before you can cash in!")
            return
        }
        var beatenTo = 0
        while(dps >= 0){ //check how many of the coins to be cashed in have already been cashed in by another user
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
        if(beatenTo > 0){ //inform the player why they're not getting the full gold sum
            toast("Unfortunately, another player has already cashed $beatenTo of these Coins. I'll only cash non-duplicates.")
        }
        if(dps>0 && todayCoins+dps <= 25){ //make sure we're cashing in a legal amount of coins
            for(a in depositBox){ //sum the amount of gold value for each coin
                if(a.currency == "QUID"){
                    goldSumInDeposit += (a.value*quid)
                }
                if(a.currency == "DOLR"){
                    goldSumInDeposit += (a.value*dolr)
                }
                if(a.currency == "SHIL"){
                    goldSumInDeposit += (a.value*shil)
                }
                if(a.currency == "PENY"){
                    goldSumInDeposit += (a.value*peny)
                }
            }
            val myGSON = Gson()
            val json = myGSON.toJson(collectedCoins)
            mRootRef.child("users").child(userName).child("collectedCoins").setValue(json) //set collectedCoins to the new value (collectedCoins-depositBox)
            mRootRef.child("users").child(userName).child("coinsToday").setValue(todayCoins+dps) //update the number of coins the player has cashed in today
            depositBox.addAll(alreadyCashed) //because all the coins in deposit box have now been cashed, they should be added to the alreadycashed list online
            mRootRef.child("days/${sdf.format(Date())}/alreadyCashedCoins").setValue(myGSON.toJson(depositBox)) //this is carried out here
            mRootRef.child("users").child(userName).child("netWorth").setValue(goldSum+goldSumInDeposit) //update the suers net worth
            depositBox.clear()
            goldSumInDeposit = 0.0//update the values and clear the used data
            setTextViews()
        }else if(dps == 0){ //if the user was cashing in only alreadycashed coins, we just need to update the server with the coins they didn't ry to cash in
            val myGSON = Gson()
            val json = myGSON.toJson(collectedCoins)
            mRootRef.child("users").child(userName).child("collectedCoins").setValue(json)
            depositBox.clear()
            setTextViews()
            toast("Looks like someone beat you to all of your coins! Unlucky!") //mock them
        }else{
            toast("You're trying to cash in ${todayCoins+dps-25} too many coins, please remove some from your deposit box!") //warn them that they are trying to cash in too many coins
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun createListView(){ //used to draw the list view at initialisation time
        listView = findViewById(R.id.listViewWallet)
        listView.adapter = CoinAdapter(this, collectedCoins)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val x:Coin = collectedCoins[position]
            if(sdf.format(Date()) == x.date){
                collectedCoins.remove(x)
                depositBox.add(x)
                listView.getItemAtPosition(position)
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
                "\n\nYou can cash in up to 25 coins every day, so if you collect more you'll either have to let them expire or trade them to a friend! To trade coins, open your profile from the main menu, " +
                "or the button below.\n\nTo cash in coins you must be at the bank! It is located at the library, however you've got to be quick, as every coin may only be banked once per day. So" +
                " if another player gets there first, you won't get any gold!\n\nBe aware that you must be at the bank in the map screen, otherwise Coinz won't know you're there!" +
                "\n\nTo remove coins from your deposit box, simply tap it!\n\nHave fun and collect fast!")
        builder.setPositiveButton("Got it!", DialogInterface.OnClickListener(positiveButtonClick))
        builder.create()
        builder.show()
    }
}
