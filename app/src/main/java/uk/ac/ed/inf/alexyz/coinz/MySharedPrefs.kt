package uk.ac.ed.inf.alexyz.coinz

import android.content.Context

class MySharedPrefs(context: Context){

    val PREFERENCE_NAME = "WALLET"
    val GOLD_VALUE = "GoldSum"
    val TODAY_DATE = "Today"
    val CURRENT_GEOJSON = "TodayGEOJSON"
    val COLLECTED_COINS = "CollectedCoins"
    val REMAINING_COINS = "RemainingCoins"
    val RECENT_EMAIL = "RecentEmail"
    val RECENT_PASSWORD = "RecentPass"
    val SHIL_RATE = "shilRate"
    val DOLR_RATE = "dolrRate"
    val PENY_RATE = "penyRate"
    val QUID_RATE = "quidRate"

    val zeroFloat:Float = 0.toFloat()

    val preference = context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_PRIVATE)

    fun getQUID():Float{
        return preference.getFloat(QUID_RATE,0.toFloat())
    }

    fun getPENY():Float{
        return preference.getFloat(PENY_RATE,0.toFloat())
    }

    fun getDOLR():Float{
        return preference.getFloat(DOLR_RATE,0.toFloat())
    }

    fun getSHIL():Float{
        return preference.getFloat(SHIL_RATE,0.toFloat())
    }

    fun setRates(quid:Float,dolr:Float,shil:Float,peny:Float){
        val editor = preference.edit()
        editor.putFloat(QUID_RATE,quid)
        editor.putFloat(PENY_RATE,peny)
        editor.putFloat(SHIL_RATE,shil)
        editor.putFloat(DOLR_RATE,dolr)
        editor.apply()
    }

    fun getEmail():String{
        return preference.getString(RECENT_EMAIL,"")
    }

    fun setEmail(email:String){
        val editor = preference.edit()
        editor.putString(RECENT_EMAIL,email)
        editor.apply()
    }

    fun getPassword():String{
        return preference.getString(RECENT_PASSWORD,"")
    }

    fun setPassword(password:String){
        val editor = preference.edit()
        editor.putString(RECENT_PASSWORD,password)
        editor.apply()
    }

    fun getGoldSum() : Float{
        return preference.getFloat(GOLD_VALUE,zeroFloat)
    }

    fun setGoldSum(gold:Float){
        val editor = preference.edit()
        editor.putFloat(GOLD_VALUE,gold)
        editor.apply()
    }

    fun getToday():String{
        return preference.getString(TODAY_DATE,"yyyy/MM/dd")

    }
    fun setToday(today:String){
        val editor = preference.edit()
        editor.putString(TODAY_DATE,today)
        editor.putString(COLLECTED_COINS,"")
        editor.putString(CURRENT_GEOJSON,"")
        editor.apply()
    }

    fun getTodayGEOJSON():String{
        return preference.getString(CURRENT_GEOJSON,"")
    }

    fun setTodayGEOJSON(geojson:String){
        val editor = preference.edit()
        editor.putString(CURRENT_GEOJSON,geojson)
        editor.apply()
    }

    fun getCollectedCoins():String{
        return preference.getString(COLLECTED_COINS, "")
    }

    fun setCollectedCoins(collectedCoins:String){
        val editor = preference.edit()
        editor.putString(COLLECTED_COINS,collectedCoins)
        editor.apply()
    }

    fun getRemainingCoins():String{
        return preference.getString(REMAINING_COINS, "")
    }

    fun setRemainingCoins(collectedMarkers:String){
        val editor = preference.edit()
        editor.putString(REMAINING_COINS,collectedMarkers)
        editor.apply()
    }
}