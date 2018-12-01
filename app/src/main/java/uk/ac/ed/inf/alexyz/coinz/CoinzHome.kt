package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.support.v7.app.AlertDialog

import kotlinx.android.synthetic.main.activity_coinz_home.*
import java.util.*

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.*


import org.jetbrains.anko.toast
import org.json.JSONObject
import uk.ac.ed.inf.alexyz.coinz.R.*

import java.text.SimpleDateFormat

class CoinzHome : AppCompatActivity() {

    private val tag = "CoinzHome"

    private  var goldSum: Double = 0.0

    private lateinit var mAuth: FirebaseAuth

    private lateinit var myDataBase: FirebaseDatabase

    private lateinit var mRootRef: DatabaseReference

    private val sdf = SimpleDateFormat("yyyy/MM/dd")

    private lateinit var userName: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_coinz_home)
        val mypref = MySharedPrefs(this)
        getGeoJSON()
        mAuth = FirebaseAuth.getInstance()
        myDataBase = FirebaseDatabase.getInstance()
        mRootRef = myDataBase.reference
        userName = mAuth.currentUser?.uid ?: ""
        mRootRef.child("users/"+userName+"/netWorth").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0!!.exists()){
                    goldSum = p0.value.toString().toDouble()
                }else{
                    toast("You have no money yet! Go get some Coinz!")
                }
            }
        })
        playButton.setOnClickListener { view ->
            if (mypref.getTodayGEOJSON() == "" || mypref.getToday() != sdf.format(Date())){
                toast("You haven't got the map for today yet!\nRe-attempting download now.")
                getGeoJSON()
            }else {
                val mapboxintent =  Intent(this, MapBoxMain::class.java)
                startActivity(mapboxintent)
            }
        }
        walletButton.setOnClickListener{view->
            startActivity(Intent(this, Wallet::class.java))
        }
        settingsButton.setOnClickListener {view ->
            mRootRef.child("users").child(userName).child("netWorth").setValue(goldSum+50.0)
            toast(goldSum.toString())

        }
        tapBarMenuHome.setOnClickListener{view ->
            tapBarMenuHome.toggle()
        }
        userProfile.setOnClickListener { view ->
            mypref.setGoldSum(goldSum.toFloat())
            startUserPage()
        }
        displayRatesHome.setOnClickListener{view->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${mypref.getSHIL()}\nDOLR: ${mypref.getDOLR()}\nPENY: ${mypref.getPENY()}\nQUID: ${mypref.getQUID()}\n")
                show()
            }
        }
    }
    private fun startUserPage(){
        startActivity(Intent(this, UserProfile::class.java))
    }

    private fun getGeoJSON(){
        val mypref = MySharedPrefs(this)
        if(mypref.getToday() != sdf.format(Date()) || mypref.getTodayGEOJSON() == "") {
            mypref.setCollectedCoins("")
            mypref.setRemainingCoins("")
            mypref.setTodayGEOJSON("")
            mypref.setRates(0.toFloat(),0.toFloat(),0.toFloat(),0.toFloat())
            mypref.setToday(sdf.format(Date()))
            val currentDate = sdf.format(Date()) + "/coinzmap.geojson"
            val todaysURL = ("http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate")
            todaysURL.httpGet().responseString() { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        mapDownloadNotifier.text = getString(string.mapProgressTRUE)
                        mapDownloadNotifier.setTextColor(getColor(color.mapDownloadBackGroundTRUE))
                        mypref.setToday(sdf.format(Date()))
                        mypref.setTodayGEOJSON(result.get())
                        val json = JSONObject(result.get())
                        val rates:JSONObject = json.getJSONObject("rates")
                        mypref.setRates(
                                rates.getDouble("QUID").toFloat(),
                                rates.getDouble("DOLR").toFloat(),
                                rates.getDouble("SHIL").toFloat(),
                                rates.getDouble("PENY").toFloat()
                        )
                    }
                    is Result.Failure -> {
                        toast("Failed to download today's map.")
                    }
                }
            }
        }else{
            mapDownloadNotifier.text = getString(string.mapProgressTRUE)
            mapDownloadNotifier.setTextColor(getColor(color.mapDownloadBackGroundTRUE))
        }
    }

    override fun onStart() {
        val mypref = MySharedPrefs(this)
        super.onStart()
        if(mypref.getToday() != sdf.format(Date())){
            toast("Your map is now out of date, updating now")
            getGeoJSON()
        }
    }
}
