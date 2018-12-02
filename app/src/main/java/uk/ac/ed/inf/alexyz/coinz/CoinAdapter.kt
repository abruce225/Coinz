package uk.ac.ed.inf.alexyz.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.wallet_row.view.*

class CoinAdapter(context: Context, collectedCoins: ArrayList<Coin>): BaseAdapter() {
    private val mContext: Context = context
    private var mcollectedCoins: ArrayList<Coin> = collectedCoins
    private val quid: Float
    private val dolr: Float
    private val shil: Float
    private val peny: Float

    init{
        val sharedPrefs = MySharedPrefs(mContext)
        quid = sharedPrefs.getQUID()
        dolr = sharedPrefs.getDOLR()
        shil = sharedPrefs.getSHIL()
        peny = sharedPrefs.getPENY()
    }

    @SuppressLint("ViewHolder", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val rowMain = layoutInflater.inflate(R.layout.wallet_row,viewGroup,false)
        rowMain.coinID.text = mcollectedCoins[position].id
        if(mcollectedCoins[position].currency=="QUID"){
            rowMain.coinMarker.setImageResource(R.drawable.ic_yellowmarker)
            rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " QUID = " +  (mcollectedCoins[position].value * quid) + " Gold Value"
        }
        if(mcollectedCoins[position].currency=="DOLR"){
            rowMain.coinMarker.setImageResource(R.drawable.ic_greenmarker)
            rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " DOLR = " +  (mcollectedCoins[position].value * dolr) + " Gold Value"
        }
        if(mcollectedCoins[position].currency=="SHIL"){
            rowMain.coinMarker.setImageResource(R.drawable.ic_bluemarker)
            rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " SHIL = " +  (mcollectedCoins[position].value * shil) + " Gold Value"
        }
        if(mcollectedCoins[position].currency=="PENY"){
            rowMain.coinMarker.setImageResource(R.drawable.ic_redmarker)
            rowMain.coinValue.text = mcollectedCoins[position].value.toString() + " PENY = " +  (mcollectedCoins[position].value * peny) + " Gold Value"
        }
        return rowMain
    }

    override fun getItem(position: Int): Any {
        notifyDataSetChanged()
        return "succesfully updated"
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return mcollectedCoins.size
    }
}