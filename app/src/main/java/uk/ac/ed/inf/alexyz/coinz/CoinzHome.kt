package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.content.Intent

import kotlinx.android.synthetic.main.activity_coinz_home.*
import kotlinx.android.synthetic.main.content_coinz_home.*

import org.jetbrains.anko.toast

class CoinzHome : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinz_home)
        setSupportActionBar(toolbar)

        playButton.setOnClickListener { view ->
            toast("I'm working!")
            val mapboxintent = Intent(this, MapBoxMain::class.java)
            startActivity(mapboxintent)
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
