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

        var tally = 0

        upvote.setOnClickListener { view ->
            tally++
            TallyValue.text = tally.toString()
            Snackbar.make(view, "I'm glad you liked it!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        downvote.setOnClickListener { view ->
            tally--
            TallyValue.text = tally.toString()
            Snackbar.make(view, "Sorry you didn't like our service.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        secondAct.setOnClickListener { view ->
            toast("Into the breach!")
            val intent1 = Intent (this, QuickBounce :: class.java)
            intent1.putExtra(QuickBounce.COUNT, tally)
            startActivity (intent1)
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
