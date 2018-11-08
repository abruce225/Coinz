package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_wallet.*

class Wallet : AppCompatActivity() {

    private lateinit var collectedCoins:ArrayList<Coin>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        getCollected()
        recyclerViewWallet.layoutManager = LinearLayoutManager(this)
        recyclerViewWallet.adapter = WalletRecycler(collectedCoins)
    }
    private fun getCollected(){
        val sharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = sharedPrefs.getCollectedCoins()
        if (json != "") {
            val coinType = object : TypeToken<List<Coin>>() {}.type
            collectedCoins = myGSON.fromJson(json, coinType)
        } else {
            collectedCoins.clear()
        }
    }
}
