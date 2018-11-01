package uk.ac.ed.inf.alexyz.coinz

//android
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import com.github.kittinunf.fuel.android.core.Json
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

//mapbox
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode

import kotlinx.android.synthetic.main.activity_map_box_main.*
import kotlinx.android.synthetic.main.content_map_box_main.*


import java.security.Permission
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.ed.inf.alexyz.coinz.R.drawable.*
import java.lang.reflect.Type


class MapBoxMain : AppCompatActivity(), PermissionsListener, LocationEngineListener {



    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location
    private lateinit var todayGJS: String


    private var myCoins = arrayListOf<Coin>()
    private var collectedCoins = arrayListOf<Coin>()
    private var remainingCoins = arrayListOf<Coin>()

    private var remainingMarkers = arrayListOf<Marker>()

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs: MySharedPrefs = MySharedPrefs(this)

        setContentView(R.layout.activity_map_box_main)
        todayGJS = sharedPrefs.getTodayGEOJSON()
        setSupportActionBar(toolbar)
        if (sharedPrefs.getCollectedCoins() != "" || sharedPrefs.getRemainingCoins() != ""){
            toast("henlo")
            getCollected()
            getRemaining()
        }
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync{ mapboxMap ->
            map = mapboxMap
            enableLocation()
            if(collectedCoins.size == 0 || remainingCoins.size == 0){
                createPins(todayGJS)
            }
            dropPins()
        }
        displayRates.setOnClickListener{view ->
            if(remainingMarkers.size > 0){
                removeMarker(remainingMarkers[0],remainingCoins[0])
            }  else {
                toast("all markers collected")
            }
        }
        floatingActionButton.setOnClickListener{view ->

        }
    }

    private fun dropPins(){
        remainingMarkers.clear()
        for (a in remainingCoins){
            when {
                a.currency.equals("PENY") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_redmarker)
                    remainingMarkers.add(map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}"))) }

                a.currency.equals("SHIL") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_bluemarker)
                    remainingMarkers.add(map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}"))) }

                a.currency.equals("QUID") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_yellowmarker)
                    remainingMarkers.add(map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}"))) }

                a.currency.equals("DOLR") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_greenmarker)
                    remainingMarkers.add(map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}"))) }
            }
        }
    }

    private fun removeMarker(marker:Marker, coin: Coin){
        map.removeMarker(marker)
        remainingMarkers.remove(marker)
        collectedCoins.add(coin)
        remainingCoins.remove(coin)
    }

    private fun setCollected(){
        val sharedPrefs: MySharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = myGSON.toJson(collectedCoins)
        sharedPrefs.setCollectedCoins(json)

    }

    private fun getCollected(){
        val sharedPrefs: MySharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = sharedPrefs.getCollectedCoins()
        if(json != ""){
            val coinType = object : TypeToken<List<Coin>>() {}.type
            collectedCoins = myGSON.fromJson(json,coinType)
        }else{
            collectedCoins.clear()
        }

    }


    private fun setRemaining(){
        val sharedPrefs: MySharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = myGSON.toJson(remainingCoins)
        sharedPrefs.setRemainingCoins(json)
    }

    private fun getRemaining(){
        val sharedPrefs: MySharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = sharedPrefs.getRemainingCoins()
        if (json != ""){
            val coinType = object : TypeToken<List<Coin>>() {}.type
            remainingCoins = myGSON.fromJson(json,coinType)
        }else{
            remainingCoins.clear()
        }

    }

    private fun createPins(todayGJS:String){
        myCoins.clear()
        val json = JSONObject(todayGJS)
        val features: JSONArray = json.getJSONArray("features")
        val featuresCount = features.length() - 1
        for (i in 0..featuresCount){
            val feature: JSONObject = features.getJSONObject(i)
            val properties: JSONObject = feature.getJSONObject("properties")
            val id: String = properties.getString("id")
            val value: Double = properties.getDouble("value")
            val currency: String = properties.getString("currency")
            val markerS: Int = properties.getInt("marker-symbol")
            val geometry: JSONObject = feature.getJSONObject("geometry")
            val holder: JSONArray = geometry.getJSONArray("coordinates")
            val longitude: Double = holder.getDouble(0)
            val latitude: Double = holder.getDouble(1)
            val coinLatLng: LatLng = LatLng(latitude,longitude)
            val tempCOin = Coin(id,currency,value,coinLatLng)
            myCoins.add(tempCOin)
        }
        remainingCoins = myCoins
    } //this function creates all 50 coins from the provided GEOJSON, and adds them to myCOins, and remainingCoins

    private fun enableLocation(){
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initialiseLocationEngine(){
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.addLocationEngineListener(this)
        locationEngine?.apply {
            interval = fastestInterval
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine?.lastLocation
        if (lastLocation != null){
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initialiseLocationLayer(){
        locationLayerPlugin = LocationLayerPlugin(mapView,map,locationEngine)
        locationLayerPlugin?.setLocationLayerEnabled(true)
        locationLayerPlugin?.cameraMode = CameraMode.TRACKING
        locationLayerPlugin?.renderMode = RenderMode.NORMAL

    }

    private fun setCameraPosition(location: Location){
        map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngine?.requestLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            originLocation = location
            setCameraPosition(location)
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        toast("Plz")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()
    }

    override fun onPause() {
        super.onPause()
        locationEngine?.removeLocationUpdates()
        mapView.onPause()
    }

    @SuppressWarnings("MissingPermission")
    override fun onResume() {
        super.onResume()
        locationEngine?.requestLocationUpdates()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        locationEngine?.removeLocationUpdates()
        locationEngine?.deactivate()
        mapView.onStop()
        setRemaining()
        setCollected()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine?.deactivate()
        mapView.onDestroy()
        setRemaining()
        setCollected()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

private class Coin(val id:String,val currency:String,val value:Double,val latLng: LatLng ){

}