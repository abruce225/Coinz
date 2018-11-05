package uk.ac.ed.inf.alexyz.coinz

//android
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

//mapbox
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
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
import kotlinx.android.synthetic.main.activity_coinz_home.*

import kotlinx.android.synthetic.main.activity_map_box_main.*

import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.ed.inf.alexyz.coinz.R.drawable.*
import java.text.SimpleDateFormat
import java.util.*

class MapBoxMain : AppCompatActivity(), PermissionsListener, LocationEngineListener {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location
    private lateinit var todayGJS: String

    private var collectedCoins = arrayListOf<Coin>()
    private var remainingCoins = arrayListOf<Coin>()
    private var remainingCoinsAndMarkers = arrayListOf<CoinAndMarker>()

    private val sdf = SimpleDateFormat("yyyy/MM/dd")

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_box_main)
        val sharedPrefs = MySharedPrefs(this)
        todayGJS = sharedPrefs.getTodayGEOJSON()
        if (sharedPrefs.getCollectedCoins() != "" || sharedPrefs.getRemainingCoins() != ""){
            getCoins()
        }
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync{ mapboxMap ->
            map = mapboxMap
            enableLocation()
            if((collectedCoins.size == 0 && remainingCoins.size == 0)){
                createPins()
            }
            dropPins()
            map.uiSettings.setCompassMargins(100,150,100,100)
            map.uiSettings.setCompassFadeFacingNorth(false)
        }
        tapBarMenu.setOnClickListener{view ->
            tapBarMenu.toggle()
        }
        openWallet.setOnClickListener{view ->
            toast("TODO OPEN WALLET")
        }
        resetDay.setOnClickListener{view ->
            resetCoins()
        }
        collectRandom.setOnClickListener{view->
            if(remainingCoinsAndMarkers.size > 0){
                removeMarker(remainingCoinsAndMarkers[0])
            }  else {
                toast("all markers collected")
            }
        }
        displayRates.setOnClickListener{view->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${sharedPrefs.getSHIL()}\nDOLR: ${sharedPrefs.getDOLR()}\nPENY: ${sharedPrefs.getPENY()}\nQUID: ${sharedPrefs.getQUID()}\n")
                show()
            }
        }

    }

    private fun dropPins(){
        remainingCoinsAndMarkers.clear()
        for (a in remainingCoins){
            when {
                a.currency.equals("PENY") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_redmarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) }

                a.currency.equals("SHIL") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_bluemarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) }

                a.currency.equals("QUID") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_yellowmarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) }

                a.currency.equals("DOLR") ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_greenmarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) }
            }
        }
        remainingCoins.clear()
    }

    private fun createPins(){
        remainingCoins.clear()
        val json = JSONObject(todayGJS)
        val features: JSONArray = json.getJSONArray("features")
        val featuresCount = features.length() - 1
        for (i in 0..featuresCount){
            val feature: JSONObject = features.getJSONObject(i)
            val properties: JSONObject = feature.getJSONObject("properties")
            val id: String = properties.getString("id")
            val value: Double = properties.getDouble("value")
            val currency: String = properties.getString("currency")
            val geometry: JSONObject = feature.getJSONObject("geometry")
            val holder: JSONArray = geometry.getJSONArray("coordinates")
            val longitude: Double = holder.getDouble(0)
            val latitude: Double = holder.getDouble(1)
            val coinLatLng = LatLng(latitude,longitude)
            val tempCoin = Coin(id,currency,value,coinLatLng)
            remainingCoins.add(tempCoin)
        }
    } //this function creates all 50 coins from the provided GEOJSON, and adds them to remainingCoins

    private fun checkIfNear(location:LatLng){
        var counter = remainingCoinsAndMarkers.size - 1
        while (counter >= 0){
            if (remainingCoinsAndMarkers[counter].coin.latLng.distanceTo(location) < 25.0){
                collectCoin(remainingCoinsAndMarkers[counter])
            }
            counter--
        }
        setCoins()
    }

    private fun collectCoin(coinAndMarker: CoinAndMarker){
        toast("Removing marker with ID: ${coinAndMarker.coin.id}")
        removeMarker(coinAndMarker)
    }

    private fun removeMarker(coinAndMarker: CoinAndMarker){
        map.removeMarker(coinAndMarker.marker)
        collectedCoins.add(coinAndMarker.coin)
        remainingCoinsAndMarkers.remove(coinAndMarker)
    }

    private fun getCoins(){
        val sharedPrefs = MySharedPrefs(this)
        if (sdf.format(Date())==sharedPrefs.getToday()){
            getRemaining()
            getCollected()
        }else{
            toast("You don't have the current map.\nPlease return to the main menu to download it.")
            sharedPrefs.setRemainingCoins("")
            sharedPrefs.setCollectedCoins("")
            sharedPrefs.setTodayGEOJSON("")
        }
    }

    private fun setCoins(){
        val sharedPrefs = MySharedPrefs(this)
        if (sdf.format(Date())==sharedPrefs.getToday()){
            setRemaining()
            setCollected()
        }else{
            toast("You don't have the current map.\nPlease return to the main menu to download it.")
            sharedPrefs.setRemainingCoins("")
            sharedPrefs.setCollectedCoins("")
            sharedPrefs.setTodayGEOJSON("")
        }
    }

    private fun setCollected(){
        for (a in remainingCoinsAndMarkers){
            remainingCoins.add(a.coin)
        }
        val sharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = myGSON.toJson(collectedCoins)
        sharedPrefs.setCollectedCoins(json)
        remainingCoins.clear()

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


    private fun setRemaining(){
        val sharedPrefs = MySharedPrefs(this)
        for (a in remainingCoinsAndMarkers){remainingCoins.add(a.coin)}
        val myGSON = Gson()
        val json = myGSON.toJson(remainingCoins)
        sharedPrefs.setRemainingCoins(json)
        remainingCoins.clear()
    }

    private fun getRemaining(){
        val sharedPrefs = MySharedPrefs(this)
        val myGSON = Gson()
        val json = sharedPrefs.getRemainingCoins()
        if (json != ""){
            val coinType = object : TypeToken<List<Coin>>() {}.type
            remainingCoins = myGSON.fromJson(json,coinType)
        }else{
            remainingCoins.clear()
        }

    }

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
            checkIfNear(LatLng(location.latitude,location.longitude))
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

    private fun resetCoins(){
        val sharedPrefs = MySharedPrefs(this)
        sharedPrefs.setRemainingCoins("")
        sharedPrefs.setCollectedCoins("")
        for (a in remainingCoinsAndMarkers){map.removeMarker(a.marker)}
        collectedCoins.clear()
        remainingCoins.clear()
        remainingCoinsAndMarkers.clear()
        createPins()
        dropPins()
    }
}

private class Coin(val id:String,val currency:String,val value:Double,val latLng: LatLng )

private class CoinAndMarker(val coin:Coin, val marker:Marker)