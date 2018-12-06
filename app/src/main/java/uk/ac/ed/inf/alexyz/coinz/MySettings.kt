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
        if (mySharedPrefs.getPP()){ //display the setting as the user has stored it in sharedprefs
            if(!popupBlocker.isChecked){
                popupBlocker.toggle()
            }
        }
        popupBlocker.text = getString(R.string.popupsettings)
        confirmChanges.setOnClickListener{ //if the user clicks the button, update the sharedPrefs to reflect the settings
            mySharedPrefs.setPP(popupBlocker.isChecked)
        }
    }
}
