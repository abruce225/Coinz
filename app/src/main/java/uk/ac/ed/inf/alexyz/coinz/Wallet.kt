package uk.ac.ed.inf.alexyz.coinz

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.wallet_row.view.*
import java.text.FieldPosition

class Wallet : AppCompatActivity() {

    private lateinit var collectedCoins:ArrayList<Coin>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        getCollected()

        val listView = findViewById<ListView>(R.id.listViewWallet)
        listView.adapter = coinAdapter(this, collectedCoins)
        //listView.setOnItemClickListener
    }
    private fun getCollected(){
        val sharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = sharedPrefs.getCollectedCoins()
        if (json != "") {
            val coinType = object : TypeToken<List<Coin>>() {}.type
            collectedCoins = myGSON.fromJson(json, coinType)
        } else {
            collectedCoins = arrayListOf()
        }
    }
    private class coinAdapter(context: Context, collectedCoins: ArrayList<Coin>): BaseAdapter() {
        private val mContext: Context
        private val mcollectedCoins: ArrayList<Coin>

        init{
            mContext = context
            mcollectedCoins = collectedCoins
        }
        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val rowMain = layoutInflater.inflate(R.layout.wallet_row,viewGroup,false)
            rowMain.coinID.setText(mcollectedCoins[position].id)
            rowMain.coinValue.setText((mcollectedCoins[position].id))
            if(mcollectedCoins[position].currency=="QUID"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_yellowmarker)
            }
            if(mcollectedCoins[position].currency=="DOLR"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_greenmarker)
            }
            if(mcollectedCoins[position].currency=="SHIL"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_bluemarker)
            }
            if(mcollectedCoins[position].currency=="PENY"){
                rowMain.coinMarker.setImageResource(R.drawable.ic_redmarker)
            }
            return rowMain
        }

        override fun getItem(position: Int): Any {
            return "Test String"
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return mcollectedCoins.size
        }

    }
}
