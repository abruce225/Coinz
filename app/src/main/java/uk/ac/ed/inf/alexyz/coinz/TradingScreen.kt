package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_trading_screen.*
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class TradingScreen : AppCompatActivity() {
    private lateinit var collectedCoins:ArrayList<Coin>

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

    private var quid: Float = 0.toFloat()
    private var dolr: Float = 0.toFloat()
    private var shil: Float = 0.toFloat()
    private var peny: Float = 0.toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trading_screen)
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
        setTextViews()
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
    }

    @SuppressLint("ResourceAsColor")
    private fun createListView(){
        listView = findViewById(R.id.tradingListView)
        listView.adapter = CoinAdapter(this, collectedCoins)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            createTradePopup(position)
            setTextViews()
        }
    }
    private fun createTradePopup(pos:Int){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Trade Coin to Player")
        builder.setMessage("Type unique trading name of the player you wish to send this coin to.\nCoin ID: ${collectedCoins[pos].id}")
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        val positiveButtonClick = { _: DialogInterface, _: Int ->
            tradeToPlayer(pos,input.text.toString())
        }
        val negativeButtonClick = { _: DialogInterface, _: Int ->
            Toast.makeText(applicationContext, "Returned to your Wallet.", Toast.LENGTH_SHORT).show()
        }
        builder.setView(input)
        builder.setPositiveButton("Confirm and Send", DialogInterface.OnClickListener(function = positiveButtonClick))
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener(function = negativeButtonClick))
        builder.create()
        builder.show()
    }
    private fun tradeToPlayer(pos:Int, target:String){
        var targetUID: String = ""
        mRootRef.child("trading/$target").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){
                    targetUID = p0.value.toString()
                    getTargetCollectedAndAdd(pos,targetUID)
                    return
                }else{
                    toast("Cannot find player with this name, please check details and try again.")
                    return
                }
            }
        })
    }
    private fun getTargetCollectedAndAdd(pos:Int,tUID:String){
        val myGSON = Gson()
        val xyz: ArrayList<Coin> = arrayListOf(collectedCoins[pos])
        mRootRef.child("users/$tUID/collectedCoins").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists() && p0.value.toString() != ""){
                    val json = p0.value.toString()
                    val coinType = object : TypeToken<List<Coin>>() {}.type
                    val abc:ArrayList<Coin> = myGSON.fromJson(json, coinType)
                    var myBool:Boolean = false
                    for(a in abc){
                        if (a.id == xyz[0].id){
                            myBool = true
                            break
                        }
                    }
                    if(myBool){
                        toast("This player already has this coin! Try sending another one.")
                        return
                    }
                    abc.addAll(xyz)
                    collectedCoins.removeAt(pos)
                    listView.getItemAtPosition(0)
                    mRootRef.child("users/$tUID/collectedCoins").setValue(myGSON.toJson(abc))
                    mRootRef.child("users/$userName/collectedCoins").setValue(myGSON.toJson(collectedCoins))
                    return
                }else{
                    collectedCoins.removeAt(pos)
                    listView.getItemAtPosition(0)
                    mRootRef.child("users/$tUID/collectedCoins").setValue(myGSON.toJson(xyz))
                    mRootRef.child("users/$userName/collectedCoins").setValue(myGSON.toJson(collectedCoins))
                    return
                }
            }
        })
    }

    private fun setTextViews(){
        tradingTextView.text = "Welcome to your tradable wallet, you have ${collectedCoins.size} coins, click a coin to begin the trading process"
    }
}
