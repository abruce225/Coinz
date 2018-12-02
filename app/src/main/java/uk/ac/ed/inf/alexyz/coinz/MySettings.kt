package uk.ac.ed.inf.alexyz.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_my_settings.*
import org.jetbrains.anko.toast

class MySettings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_settings)
        val mySharedPrefs = MySharedPrefs(this)
        if (mySharedPrefs.getPP()){
            if(!popupBlocker.isChecked){
                popupBlocker.toggle()
            }
        }
        popupBlocker.text = "Toggle Information Popups On/Off"
        confirmChanges.setOnClickListener{
            mySharedPrefs.setPP(popupBlocker.isChecked)
            toast(mySharedPrefs.getPP().toString())
        }
    }
}
