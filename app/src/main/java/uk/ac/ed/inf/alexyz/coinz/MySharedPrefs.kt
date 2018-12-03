package uk.ac.ed.inf.alexyz.coinz

import android.content.Context

class MySharedPrefs(context: Context) {

    val PREFERENCE_NAME = "WALLET"
    val TODAY_DATE = "Today"
    val CURRENT_GEOJSON = "TodayGEOJSON"
    val RECENT_EMAIL = "RecentEmail"
    val RECENT_PASSWORD = "RecentPass"
    val SHIL_RATE = "shilRate"
    val DOLR_RATE = "dolrRate"
    val PENY_RATE = "penyRate"
    val QUID_RATE = "quidRate"
    val LATI = "lat"
    val LONG = "lon"
    val POPUP = "wantsPopup"

    val preference = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)!!

    fun setPP(b:Boolean){
        val editor = preference.edit()
        editor.putBoolean(POPUP,b)
        editor.apply()
    }
    fun getPP():Boolean{

        return preference.getBoolean(POPUP,true)
    }

    fun getLAT(): String {
        return preference.getString(LATI, "0")
    }

    fun setLAT(string: String) {
        val editor = preference.edit()
        editor.putString(LATI, string)
        editor.apply()
    }

    fun getLON(): String {
        return preference.getString(LONG, "0")
    }

    fun setLON(string: String) {
        val editor = preference.edit()
        editor.putString(LONG, string)
        editor.apply()
    }

    fun getQUID(): Float {
        return preference.getFloat(QUID_RATE, 0.toFloat())
    }

    fun getPENY(): Float {
        return preference.getFloat(PENY_RATE, 0.toFloat())
    }

    fun getDOLR(): Float {
        return preference.getFloat(DOLR_RATE, 0.toFloat())
    }

    fun getSHIL(): Float {
        return preference.getFloat(SHIL_RATE, 0.toFloat())
    }

    fun setRates(quid: Float, dolr: Float, shil: Float, peny: Float) {
        val editor = preference.edit()
        editor.putFloat(QUID_RATE, quid)
        editor.putFloat(PENY_RATE, peny)
        editor.putFloat(SHIL_RATE, shil)
        editor.putFloat(DOLR_RATE, dolr)
        editor.apply()
    }

    fun getEmail(): String {
        return preference.getString(RECENT_EMAIL, "")
    }

    fun setEmail(email: String) {
        val editor = preference.edit()
        editor.putString(RECENT_EMAIL, email)
        editor.apply()
    }

    fun getPassword(): String {
        return preference.getString(RECENT_PASSWORD, "")
    }

    fun setPassword(password: String) {
        val editor = preference.edit()
        editor.putString(RECENT_PASSWORD, password)
        editor.apply()
    }

    fun getToday(): String {
        return preference.getString(TODAY_DATE, "yyyy/MM/dd")
    }
    fun setToday(date:String){
        val editor = preference.edit()
        editor.putString(TODAY_DATE,date)
        editor.apply()
    }

    fun getTodayGEOJSON(): String {
        return preference.getString(CURRENT_GEOJSON, "")
    }

    fun setTodayGEOJSON(geojson: String) {
        val editor = preference.edit()
        editor.putString(CURRENT_GEOJSON, geojson)
        editor.apply()
    }
}