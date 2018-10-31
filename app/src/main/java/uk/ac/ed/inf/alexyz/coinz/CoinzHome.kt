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


import org.jetbrains.anko.toast

import java.text.SimpleDateFormat

class CoinzHome : AppCompatActivity() {

    private val tag = "CoinzHome"

    private var tempFile: String? = null

    private  var goldSum: Float? = null

    private val sdf = SimpleDateFormat("yyyy/MM/dd")

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinz_home)
        setSupportActionBar(toolbar)
        val mypref = MySharedPrefs(this)
        mypref.setToday(sdf.format(Date()))
        getGeoJSON()
        playButton.setOnClickListener { view ->
            if (tempFile == null){
                toast("You haven't got the map for today yet!")
            }else {
                val mapboxintent =  Intent(this, MapBoxMain::class.java)
                mapboxintent.putExtra("GEOJSON", tempFile)
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
        val currentDate = sdf.format(Date()) + "/coinzmap.geojson"
        val todaysURL = ("http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate")
        todaysURL.httpGet().responseString(){request,response,result->
            when(result){
                is Result.Success -> {
                    toast("Todays map downloaded successfully!")
                    tempFile = result.get()
                }
                is Result.Failure -> {toast("Failed to download today's map.")}
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
