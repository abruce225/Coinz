package uk.ac.ed.inf.alexyz.coinz

import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.support.v7.app.AlertDialog

import kotlinx.android.synthetic.main.activity_coinz_home.*
import java.util.*

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_map_box_main.*


import org.jetbrains.anko.toast
import org.json.JSONObject
import uk.ac.ed.inf.alexyz.coinz.R.*

import java.text.SimpleDateFormat

class CoinzHome : AppCompatActivity() {

    private val tag = "CoinzHome"

    private  var goldSum: Float? = null

    private lateinit var mAuth: FirebaseAuth

    private val sdf = SimpleDateFormat("yyyy/MM/dd")


    override fun onCreate(savedInstanceState: Bundle?) {

        val mypref = MySharedPrefs(this)
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_coinz_home)
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
            toast(mypref.getPENY().toString())
        }
        settingsButton.setOnClickListener {view ->
            mypref.addGold(35.toFloat())
            goldSum = mypref.getGoldSum()
            toast(goldSum.toString())
        }
        tapBarMenuHome.setOnClickListener{view ->
            tapBarMenuHome.toggle()
        }
        userProfile.setOnClickListener { view ->
            startActivity(Intent(this, UserProfile::class.java))
        }
        displayRatesHome.setOnClickListener{view->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${mypref.getSHIL()}\nDOLR: ${mypref.getDOLR()}\nPENY: ${mypref.getPENY()}\nQUID: ${mypref.getQUID()}\n")
                show()
            }
        }
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
        super.onStart()
        getGeoJSON()
        mAuth = FirebaseAuth.getInstance()
    }
}
