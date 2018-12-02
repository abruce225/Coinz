package uk.ac.ed.inf.alexyz.coinz

//android
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userName: String
    private lateinit var mRootRef: DatabaseReference



    private var collectedCoins = arrayListOf<Coin>()
    private var remainingCoins = arrayListOf<Coin>()
    private var remainingCoinsAndMarkers = arrayListOf<CoinAndMarker>()

    private val sdf = SimpleDateFormat("yyyy/MM/dd")


    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_box_main)
        mAuth = FirebaseAuth.getInstance()
        mRootRef = FirebaseDatabase.getInstance().reference
        val sharedPrefs = MySharedPrefs(this)
        todayGJS = sharedPrefs.getTodayGEOJSON()
        userName = mAuth.currentUser?.uid ?: ""
        mRootRef.child("users/$userName/collectedCoins").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0!!.exists()){
                    val myGSON = Gson()
                    val json = p0.value.toString()
                    if (json != "") {
                        val coinType = object : TypeToken<List<Coin>>() {}.type
                        collectedCoins = myGSON.fromJson(json, coinType)
                    } else {
                        collectedCoins.clear()
                    }

                }
            }
        })
        mRootRef.child("users/$userName/remainingCoins").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0!!.exists()) {
                    val myGSON = Gson()
                    val json = p0.value.toString()
                    if (json != "") {
                        val coinType = object : TypeToken<List<Coin>>() {}.type
                        remainingCoins = myGSON.fromJson(json, coinType)
                    } else {
                        remainingCoins.clear()
                    }
                }
            }
        })
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

        }
        collectRandom.setOnClickListener{view->
            if(remainingCoinsAndMarkers.size > 0){
                collectCoin(remainingCoinsAndMarkers[0])
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
        val myIcon = IconFactory.getInstance(this).fromResource(R.mipmap.ic_bank_icon)
        map.addMarker(MarkerOptions().position(LatLng(55.942963,-3.189014)).icon(myIcon).title("$$$ Central Bank $$$"))
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
            val tempCoin = Coin(id,currency,value,coinLatLng,sdf.format(Date()))
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
        setCoins()
    }


    private fun setCoins(){
        val sharedPrefs = MySharedPrefs(this)
        if (sdf.format(Date())==sharedPrefs.getToday()){
            setRemaining()
            setCollected()
        }else{
            toast("You don't have the current map.\nReturning to the main menu to download it now.")
            finish()
        }
    }

    private fun setCollected() {
        val myGSON = Gson()
        val json = myGSON.toJson(collectedCoins)
        mRootRef.child("users").child(userName).child("collectedCoins").setValue(json)
    }

    private fun setRemaining(){
        remainingCoins.clear()
        for (a in remainingCoinsAndMarkers){remainingCoins.add(a.coin)}
        val myGSON = Gson()
        val json = myGSON.toJson(remainingCoins)
        mRootRef.child("users").child(userName).child("remainingCoins").setValue(json)
        remainingCoins.clear()
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
        val lifecycle = lifecycle
        lifecycle.addObserver(locationLayerPlugin!!)
    }

    private fun setCameraPosition(location: Location){
        map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngine?.requestLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
        val mySharedPrefs = MySharedPrefs(this)
        location?.let {
            originLocation = location
            mySharedPrefs.setLAT(location.latitude.toString())
            mySharedPrefs.setLON(location.longitude.toString())
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
        mapView.onStart()
        super.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        if (locationEngine != null) {
            try {
                locationEngine!!.requestLocationUpdates()
            } catch (ignored: SecurityException) {
            }
            locationEngine!!.addLocationEngineListener(this)
        }

    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationEngine?.removeLocationUpdates()
    }

    @SuppressWarnings("MissingPermission")
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        locationEngine?.requestLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        locationEngine?.removeLocationUpdates()
        locationEngine?.deactivate()
        if (locationEngine != null) {
            locationEngine!!.removeLocationEngineListener(this)
            locationEngine!!.removeLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine?.deactivate()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

private class CoinAndMarker(val coin:Coin, val marker:Marker)