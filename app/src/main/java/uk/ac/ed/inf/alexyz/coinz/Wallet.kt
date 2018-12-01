package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.kittinunf.fuel.android.core.Json
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.wallet_row.view.*
import org.jetbrains.anko.toast
import java.text.FieldPosition
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*
import java.text.SimpleDateFormat


class Wallet : AppCompatActivity() {

    private lateinit var collectedCoins:ArrayList<Coin>

    private lateinit var depositBox:ArrayList<Coin>

    private lateinit var mAuth: FirebaseAuth

    private lateinit var myDataBase: FirebaseDatabase

    private lateinit var mRootRef: DatabaseReference

    private lateinit var userName: String

    private var goldSumInDeposit: Double = 0.0

    private var goldSum: Double = 0.0

    private var todayCoins: Int = 0

    private var quid: Float = 0.toFloat()
    private var dolr: Float = 0.toFloat()
    private var shil: Float = 0.toFloat()
    private var peny: Float = 0.toFloat()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        depositBox = arrayListOf()
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        val sharedPrefs = MySharedPrefs(this)
        quid = sharedPrefs.getQUID()
        dolr = sharedPrefs.getDOLR()
        shil = sharedPrefs.getSHIL()
        peny = sharedPrefs.getPENY()
        getCollected()
        createListView()
        mRootRef.child("users/"+userName+"/netWorth").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0!!.exists()){
                    goldSum = p0.value.toString().toDouble()
                }
            }
        })
        mRootRef.child("users/"+userName+"/coinsToday").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("error big problem")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0!!.exists()){
                    todayCoins = p0.value.toString().toInt()
                    toast("You've currently cashed in $todayCoins coins.")
                }else{
                    toast("You haven't banked any coins yet today!")
                }
            }
        })
        buttonSyncSelected.setOnClickListener{
            cashInAllDeposit()
        }
        textViewSubCons.setOnClickListener{
            openDepositPopup()
        }
    }
    private fun getCollected(){
        val sharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = sharedPrefs.getCollectedCoins()
        if (json != "") {
            val coinType = object : TypeToken<List<Coin>>() {}.type
            collectedCoins = myGSON.fromJson(json, coinType)
        } else {
            collectedCoins = arrayListOf()
        }
        textViewWallet.setText("Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!")
        textViewSubCons.setText("Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!")
    }

    private fun openDepositPopup(){

    }

    private fun cashInAllDeposit(){
        val sharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = myGSON.toJson(collectedCoins)
        sharedPrefs.setCollectedCoins(json)
        val jsonForFirebase = myGSON.toJson(depositBox)
        if(todayCoins+depositBox.size <= 25){
            mRootRef.child("users").child(userName).child("netWorth").setValue(goldSum+goldSumInDeposit)
            mRootRef.child("users").child(userName).child("coinsToday").setValue(todayCoins+depositBox.size)
            depositBox.clear()
            textViewSubCons.setText("Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!")
        }else{
            toast("You're trying to cash in ${todayCoins+depositBox.size-25} too many coins, please remove some from your deposit box!")
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun createListView(){
        val listView = findViewById<ListView>(R.id.listViewWallet)
        listView.adapter = coinAdapter(this, collectedCoins)
        listView.onItemClickListener = object : AdapterView.OnItemClickListener {

            override fun onItemClick(parent: AdapterView<*>, view: View,
                                     position: Int, id: Long) {
                var x:Coin = collectedCoins[position]
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
                textViewWallet.setText("Your wallet currently contain ${collectedCoins.size} coins.\nTap a row to add it to your deposit box!")
                textViewSubCons.setText("Your deposit box currently contains ${depositBox.size} coins. Tap here to remove coins from your deposit box!")
            }
        }
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
