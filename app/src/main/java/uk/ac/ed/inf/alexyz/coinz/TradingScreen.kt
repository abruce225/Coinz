package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
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
        if (sharedPrefs.getPP()){
            showInformationPopup()
        }
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
        tapBarMenuTrade.setOnClickListener{
            tapBarMenuTrade.toggle()
        }
        openWalletTrade.setOnClickListener{
            startActivity(Intent(this, Wallet::class.java))
        }
        openPopupTrade.setOnClickListener{
            showInformationPopup()
        }
        openUserProfileTrade.setOnClickListener{
            finish()
        }
        displayRatesTrade.setOnClickListener{
            AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${sharedPrefs.getSHIL()}\nDOLR: ${sharedPrefs.getDOLR()}\nPENY: ${sharedPrefs.getPENY()}\nQUID: ${sharedPrefs.getQUID()}\n")
                show()
            }
        }
    }

    private fun showInformationPopup(){
        val builder = AlertDialog.Builder(this)
        val positiveButtonClick = { _: DialogInterface, _: Int ->}
        builder.setTitle("Information for Trading")
        builder.setMessage("Welcome to the trading screen! From here you can send coins to your friends.\n" +
                "\nTo send coins, you'll need to get your friends 8 character code from their profile. Once you have this, simply click on a coin in this activity and enter the code." +
                "The coin will be automatically added to your friends account, so next time they play Coinz it'll be there waiting for them!\n" +
                "\nMake sure you send the right coin to your friend, as there's no way to get a coin back once it's sent.\n" +
                "\nBe aware that you can't send a friend a coin that they already have! Happy trading!")
        builder.setPositiveButton("Got it!", DialogInterface.OnClickListener(positiveButtonClick))
        builder.create()
        builder.show()
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
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        input.textAlignment = LinearLayout.TEXT_ALIGNMENT_CENTER
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
        var targetUID: String
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
                    var myBool = false
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

    @SuppressLint("SetTextI18n")
    private fun setTextViews(){
        tradingTextView.text = "Welcome to your tradable wallet, you have ${collectedCoins.size} coins, click a coin to begin the trading process"
    }
}
