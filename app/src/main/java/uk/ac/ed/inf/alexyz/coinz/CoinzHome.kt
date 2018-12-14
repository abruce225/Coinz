package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.support.v7.app.AlertDialog

import kotlinx.android.synthetic.main.activity_coinz_home.*
import java.util.*

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


import org.jetbrains.anko.toast
import org.json.JSONObject
import uk.ac.ed.inf.alexyz.coinz.R.*

import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

class CoinzHome : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var myDataBase: FirebaseDatabase

    private lateinit var mRootRef: DatabaseReference

    private lateinit var collectedCoins: ArrayList<Coin>

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy/MM/dd")

    private val coinType = object : TypeToken<List<Coin>>() {}.type

    private lateinit var userName: String

    private lateinit var todayDateUser : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_coinz_home)
        val sharedPrefs = MySharedPrefs(this) //initialise sharedPrefs and Firebase etc for this activity
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        if(sharedPrefs.getPP()){ //if the user has toggle the popups off, we don't show one, otherwise, display popup
            showInformationPopup()
        }
        mRootRef.child("users/$userName/date").addListenerForSingleValueEvent(object : ValueEventListener{ //listener to pull the most recent date of login from the database.
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {//execute getGeoJSOn after we know what date the user last played on
                if (p0.exists()){
                    todayDateUser = p0.value.toString()
                    getGeoJSON() //all handling of downloading the geoJSON for the day and storing it in shared prefs is handled within this function.
                }else{
                    todayDateUser = ""
                    getGeoJSON() //all handling of downloading the geoJSON for the day and storing it in shared prefs is handled within this function.
                }
            }
        })
        playButton.setOnClickListener { //group of self explanatory listeners to let the user interact with the tile interface
            if (sharedPrefs.getTodayGEOJSON() == "" || sharedPrefs.getToday() != sdf.format(Date())){ //this checks if the user has the current GeoJSON, otherwise it will try to redownload the current map
                toast("You haven't got the map for today yet!\nRe-attempting download now.")
                getGeoJSON()
            }else {
                startActivity(Intent(this, MapBoxMain::class.java))
            }
        }
        userProfile.setOnClickListener{
            startActivity(Intent(this,UserProfile::class.java))
        }
        walletButton.setOnClickListener{
            startActivity(Intent(this, Wallet::class.java))
        }
        settingsButton.setOnClickListener {
            startActivity(Intent(this,MySettings::class.java))
        }
        tapBarMenuHome.setOnClickListener{
            tapBarMenuHome.toggle()
        }
        openWalletHome.setOnClickListener{
            startActivity(Intent(this, Wallet::class.java))
        }
        openPopupHome.setOnClickListener{
            showInformationPopup()
        }
        openUserProfileHome.setOnClickListener{
            startActivity(Intent(this,UserProfile::class.java))
        }
        displayRatesHome.setOnClickListener{
            AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${sharedPrefs.getSHIL()}\nDOLR: ${sharedPrefs.getDOLR()}\nPENY: ${sharedPrefs.getPENY()}\nQUID: ${sharedPrefs.getQUID()}\n")
                show()
            }
        }
        storeButton.setOnClickListener{
            startActivity(Intent(this,MyStore::class.java))
        }
    }

    private fun getGeoJSON(){
        val sharedPrefs = MySharedPrefs(this)
        val currentDay = sdf.format(Date()) //easier to hold this here than access it a million times
        if(sharedPrefs.getToday() != currentDay || sharedPrefs.getTodayGEOJSON() == "") { //if the device has never downloaded a map before, or has a previous one stored, we wipe and reset
            sharedPrefs.setTodayGEOJSON("")
            sharedPrefs.setRates(0.toFloat(),0.toFloat(),0.toFloat(),0.toFloat())
            sharedPrefs.setToday(currentDay)
            val todaysURL = ("http://homepages.inf.ed.ac.uk/stg/coinz/$currentDay/coinzmap.geojson")//set url for geojson download
            todaysURL.httpGet().responseString { _, _, result -> //use httpGet to pull down the geojson, then parse it below. This involves adding values to shared prefs, which will then be
                when (result) {                                  //formatted into an arraylist of coins etc in the mapbox activity
                    is Result.Success -> {
                        mapDownloadNotifier.text = getString(string.mapProgressTRUE)
                        mapDownloadNotifier.setTextColor(getColor(color.mapDownloadBackGroundTRUE))
                        sharedPrefs.setToday(currentDay)
                        sharedPrefs.setTodayGEOJSON(result.get())
                        val json = JSONObject(result.get())
                        val rates:JSONObject = json.getJSONObject("rates")
                        sharedPrefs.setRates(
                                rates.getDouble("QUID").toFloat(),
                                rates.getDouble("DOLR").toFloat(),
                                rates.getDouble("SHIL").toFloat(),
                                rates.getDouble("PENY").toFloat()
                        )
                    }
                    is Result.Failure -> {
                        toast("Failed to download today's map. Please relaunch to try again")
                    }
                }
            }
        }
        else{ //otherwise we can notify the user that the map is ready in the string at the top of the home page, and let them continue
            mapDownloadNotifier.text = getString(string.mapProgressTRUE)
            mapDownloadNotifier.setTextColor(getColor(color.mapDownloadBackGroundTRUE))
            return
        }
    }
    private fun showInformationPopup(){ //simple function to display information popup that greets user when starting CoinzHome
        val builder = AlertDialog.Builder(this)
        val positiveButtonClick = { _: DialogInterface, _: Int ->}
        builder.setTitle("Information for Coinz")
        builder.setMessage("Welcome to Coinz! You're currently on the home screen, where you have fast access to everything within the app. Simply tap a tile to get started!\n" +
                "\nThere's also a button at the bottom of the screen you may press at any time, it'll bring up shortcuts to your wallet and profile, along with access to this" +
                " popup if you want to check the info again.\n\nHave fun!")
        builder.setPositiveButton("Got it!", DialogInterface.OnClickListener(positiveButtonClick))
        builder.create()
        builder.show()
    }
}
