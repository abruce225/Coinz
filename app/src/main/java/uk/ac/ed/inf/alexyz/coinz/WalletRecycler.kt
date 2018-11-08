package uk.ac.ed.inf.alexyz.coinz

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_wallet.view.*
import kotlinx.android.synthetic.main.wallet_row.view.*
import uk.ac.ed.inf.alexyz.coinz.R.*
import java.text.FieldPosition

class WalletRecycler(collectedCoins: ArrayList<Coin>): RecyclerView.Adapter<CustomViewHolder>(){

    private var collectedCoinsStored = collectedCoins

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.view.coinID?.text = collectedCoinsStored[position].id
        holder.view.coinValue.text = collectedCoinsStored[position].value.toString()
        if(collectedCoinsStored[position].currency == "QUID"){
            holder.view.coinMarker.setImageResource(drawable.ic_yellowmarker)
        }
        if(collectedCoinsStored[position].currency == "DOLR"){
            holder.view.coinMarker.setImageResource(drawable.ic_greenmarker)
        }
        if(collectedCoinsStored[position].currency == "SHIL"){
            holder.view.coinMarker.setImageResource(drawable.ic_bluemarker)
        }
        if(collectedCoinsStored[position].currency == "PENY"){
            holder.view.coinMarker.setImageResource(drawable.ic_redmarker)
        }
    }

    override fun getItemCount(): Int {
        return collectedCoinsStored.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val cellForRow = layoutInflater.inflate(layout.wallet_row,parent,false)
        return CustomViewHolder(cellForRow)
    }
}

class CustomViewHolder(val view: View): RecyclerView.ViewHolder(view){

}