package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import org.jetbrains.anko.toast

import kotlinx.android.synthetic.main.activity_quick_bounce.*
import kotlinx.android.synthetic.main.content_coinz_home.*

class QuickBounce : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_bounce)

        nobacksy.setOnClickListener { view ->
            toast("Fall back!!")
            finish()
        }
    }
}
