package uk.ac.ed.inf.alexyz.coinz

import android.content.Context

class MySharedPrefs(context: Context){

    val PREFERENCE_NAME = "WALLET"
    val GOLD_VALUE = "GoldSum"
    val TODAY_DATE = "Today"
    val zeroFloat:Float = 0.toFloat()

    val preference = context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_PRIVATE)


    fun getGoldSum() : Float{
        return preference.getFloat(GOLD_VALUE,zeroFloat)
    }

    fun addGold(gold:Float){
        val editor = preference.edit()
        var quickGold:Float = getGoldSum() + gold
        editor.putFloat(GOLD_VALUE,quickGold )
        editor.apply()
    }

    fun getToday():String{
        return preference.getString(TODAY_DATE,"yyyy/MM/dd")
    }
    fun setToday(today:String){
        val editor = preference.edit()
        editor.putString(TODAY_DATE,today)
        editor.apply()
    }
}