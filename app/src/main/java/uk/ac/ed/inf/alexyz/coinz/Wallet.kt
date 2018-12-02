package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.wallet_row.view.*
import org.jetbrains.anko.toast
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*

import com.mapbox.mapboxsdk.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


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


    @SuppressLint("MissingPermission")
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
        quid = sharedPrefs.getQUID()
        dolr = sharedPrefs.getDOLR()
        shil = sharedPrefs.getSHIL()
        peny = sharedPrefs.getPENY()
        textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
        textViewSubCons.text = "Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!"
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
                        textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
                        textViewSubCons.text = "Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!"
                        createListView()

                    }else{
                        textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
                        textViewSubCons.text = "Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!"
                        createListView()
                    }
                }else{
                    textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
                    textViewSubCons.text = "Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!"
                }
            }
        })
        mRootRef.child("days/${sdf.format(Date())}/alreadyCashedCoins").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0!!.exists()) {
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
            var mll:LatLng = LatLng(0.0,0.0)
            mll.latitude = sharedPrefs.getLAT().toDouble()
            mll.longitude = sharedPrefs.getLON().toDouble()
            toast("lat: ${mll.latitude} \nlon: ${mll.longitude}")
            if(LatLng(55.942963,-3.189014).distanceTo(mll) < 250) {
                cashInAllDeposit()
                val itemValue = listView.getItemAtPosition(0) as String
            }else{
                toast("You must be at the central bank to cash in coins!")
            }
        }
        textViewSubCons.setOnClickListener {
            openDepositPopup()
        }
    }

    private fun openDepositPopup(){
        val positiveButtonClick = { dialog: DialogInterface, which: Int ->
            returnSelectedToWallet()
            Toast.makeText(applicationContext, "Returned to your Wallet.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Your Deposit Box")
        val ids = Array<String>(depositBox.size){"it = $it"}
        for ((count, a) in depositBox.withIndex()){
            ids[count] = a.id
        }
        builder.setMultiChoiceItems(ids, null) { dialog, which, isChecked ->
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
    private fun returnSelectedToWallet(){
        for(a in returnToWallet){
            depositBox.remove(a)
            collectedCoins.add(a)
        }
        listView.getItemAtPosition(0)
        returnToWallet.clear()
        textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
        textViewSubCons.text = "Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!"
    }

    private fun cashInAllDeposit(){
        var dps: Int = depositBox.size - 1
        if(dps == -1){
            toast("You must add coins to your deposit box before you can cash in!")
            return
        }
        var beatenTo: Int = 0
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
            textViewSubCons.text = "Your deposit box currently contains 0 coins. Tap here to remove coins from your deposit box!"
        }else if(dps == 0){
            toast("Looks like someone beat you to all of your coins! Unlucky!")
        }else{
            toast("You're trying to cash in ${todayCoins+dps-25} too many coins, please remove some from your deposit box!")
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun createListView(){
        listView = findViewById<ListView>(R.id.listViewWallet)
        listView.adapter = coinAdapter(this, collectedCoins)
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            var x:Coin = collectedCoins[position]
            if(sdf.format(Date()) == x.date){
                collectedCoins.remove(x)
                depositBox.add(x)
                val itemValue = listView.getItemAtPosition(position) as String
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
            textViewWallet.text = "Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!"
            textViewSubCons.text = "Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!"
        }
    }

    private fun createExpiredPopup(){
        val positiveButtonClick = { dialog: DialogInterface, which: Int ->
            removeExpired()
            Toast.makeText(applicationContext, "Removed all expired coins.", Toast.LENGTH_SHORT).show()
        }
        val negativeButtonClick = { dialog: DialogInterface, which: Int ->
            Toast.makeText(applicationContext, "Expired coins still present in your wallet.", Toast.LENGTH_SHORT).show()
        }
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Coin Expired!")
        builder.setMessage("Unfortunately, this coin has expired. Would you like me to remove all expired coins from your wallet?")
        builder.setPositiveButton("Remove" ,DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Keep" ,DialogInterface.OnClickListener(function = negativeButtonClick))
    }

    private fun removeExpired(){
        for(coin in collectedCoins){
            if (sdf.format(Date()) != coin.date){
                collectedCoins.remove(coin)
            }
        }
        listView.getItemAtPosition(0)
    }

    private class coinAdapter(context: Context, collectedCoins: ArrayList<Coin>): BaseAdapter() {
        private val mContext: Context = context
        private var mcollectedCoins: ArrayList<Coin> = collectedCoins
        private val quid: Float
        private val dolr: Float
        private val shil: Float
        private val peny: Float

        init{
            val sharedPrefs = MySharedPrefs(mContext)
            quid = sharedPrefs.getQUID()
            dolr = sharedPrefs.getDOLR()
            shil = sharedPrefs.getSHIL()
            peny = sharedPrefs.getPENY()
        }

        @SuppressLint("ViewHolder", "SetTextI18n")
        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val rowMain = layoutInflater.inflate(R.layout.wallet_row,viewGroup,false)
            rowMain.coinID.text = mcollectedCoins[position].id
            if(mcollectedCoins[position].currency=="QUID"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_yellowmarker)
                rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " QUID = " +  (mcollectedCoins[position].value * quid) + " Gold Value"
            }
            if(mcollectedCoins[position].currency=="DOLR"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_greenmarker)
                rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " DOLR = " +  (mcollectedCoins[position].value * dolr) + " Gold Value"
            }
            if(mcollectedCoins[position].currency=="SHIL"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_bluemarker)
                rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " SHIL = " +  (mcollectedCoins[position].value * shil) + " Gold Value"
            }
            if(mcollectedCoins[position].currency=="PENY"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_redmarker)
                rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " PENY = " +  (mcollectedCoins[position].value * peny) + " Gold Value"
            }
            return rowMain
        }

        override fun getItem(position: Int): Any {
            notifyDataSetChanged()
            return "succesfully updated"
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return mcollectedCoins.size
        }
    }

}
