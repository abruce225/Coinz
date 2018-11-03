package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.content.Intent

import kotlinx.android.synthetic.main.activity_coinz_home.*
import kotlinx.android.synthetic.main.content_coinz_home.*
import java.util.*

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.firebase.auth.FirebaseAuth


import org.jetbrains.anko.toast
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
        setSupportActionBar(toolbar)
        playButton.setOnClickListener { view ->
            if (mypref.getTodayGEOJSON() == ""){
                toast("You haven't got the map for today yet!\nRe-attempting download now.")
                getGeoJSON()
            }else {
                val mapboxintent =  Intent(this, MapBoxMain::class.java)
                startActivity(mapboxintent)
            }
        }
        settingsButton.setOnClickListener {view ->
            mypref.addGold(35.toFloat())
            goldSum = mypref.getGoldSum()
            toast(goldSum.toString())
        }

    }

    private fun getGeoJSON(){
        val mypref = MySharedPrefs(this)
        val currentDate = sdf.format(Date()) + "/coinzmap.geojson"
        val todaysURL = ("http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate")
        todaysURL.httpGet().responseString(){request,response,result->
            when(result){
                is Result.Success -> {
                    mapDownloadNotifier.text = getString(string.mapProgressTRUE)
                    mapDownloadNotifier.background = getDrawable(color.mapDownloadBackGroundTRUE)
                    mypref.setTodayGEOJSON(result.get())

                }
                is Result.Failure -> {
                    toast("Failed to download today's map.")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_coinz_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        getGeoJSON()
        mAuth = FirebaseAuth.getInstance()
        toast(mAuth.currentUser?.email.toString())
    }
}
