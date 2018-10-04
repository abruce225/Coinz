package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.widget.TextView
import org.jetbrains.anko.toast


import kotlinx.android.synthetic.main.activity_quick_bounce.*
import kotlinx.android.synthetic.main.content_coinz_home.*
import java.lang.Math.random
import java.lang.Math.toIntExact

class QuickBounce : AppCompatActivity() {

    companion object {
        const val COUNT = "total_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_bounce)
        val count = intent.getIntExtra(COUNT, 0)
        val myR = (random() * count).toInt()

        textView3.text = Integer.toString(myR)

        nobacksy.setOnClickListener { view ->
            toast("Fall back!!")
            finish()
        }
    }


}
