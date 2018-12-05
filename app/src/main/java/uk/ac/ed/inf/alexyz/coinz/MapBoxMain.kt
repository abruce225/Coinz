package uk.ac.ed.inf.alexyz.coinz

//android
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
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
    private lateinit var databaseDate: String

    private var collectedCoins = arrayListOf<Coin>()
    private var remainingCoins = arrayListOf<Coin>()
    private var remainingCoinsAndMarkers = arrayListOf<CoinAndMarker>()

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy/MM/dd")
    private var distanceTo = 25


    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_box_main)
        mAuth = FirebaseAuth.getInstance() //initialise lateinit vars etc
        mRootRef = FirebaseDatabase.getInstance().reference
        val sharedPrefs = MySharedPrefs(this)
        todayGJS = sharedPrefs.getTodayGEOJSON()
        userName = mAuth.currentUser?.uid ?: ""
        if(sharedPrefs.getPP()){ //display popup if user has requested them in settings
            showInformationPopup()
        }
        mRootRef.child("users/$userName/hoover").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                toast("can't access your data right now")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){
                    distanceTo = if(p0.value.toString() == sdf.format(Date())){
                        75
                    }else{
                        25
                    }
                }
            }
        })
        mRootRef.child("users/$userName/date").addListenerForSingleValueEvent(object: ValueEventListener{ //one time pull of most recent date that the user has played the game
            override fun onCancelled(p0: DatabaseError) {
                toast("can't access your data right now")
            }
            override fun onDataChange(p0: DataSnapshot) {
                databaseDate = if(p0.exists()){ //if the user has played before, set database date to that value, otherwise set it to an empty string
                    p0.value.toString()
                }else{
                    ""
                }
            }
        })
        mRootRef.child("users/$userName/collectedCoins").addValueEventListener(object : ValueEventListener { //this value is constantly updating, with potential for someone to trade a coin in as the player is playing
            override fun onCancelled(p0: DatabaseError) { //every time it changes, we updated collected coins
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()){
                    val myGSON = Gson() //use of Gson to convert arraylist into string for storage and transfer. Gson slaps hard
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
        mRootRef.child("users/$userName/remainingCoins").addValueEventListener(object : ValueEventListener {//same as collected coins, this allows us to keep tabs on which coins are remaining
            override fun onCancelled(p0: DatabaseError) {                                                             //and in turn which coins should be rendered
                toast("Couldn't access your data, please check your net connection.")
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
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
        Mapbox.getInstance(applicationContext, getString(R.string.access_token)) //section that sets up mapbox and mapview
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync{ mapboxMap -> //once map is set up, we need to create "pins" that allow us to draw on the map
            map = mapboxMap
            enableLocation()
            if((sdf.format(Date())) != databaseDate){//if this is the users first session today, we must create a full set of pins from the geojson
                createPins()
            }
            dropPins()//otherwise we can simply use the coins stored in remaining coins on the database to drop the markers on the map
            map.uiSettings.setCompassMargins(100,150,100,100) //add compass for easy orientation
            map.uiSettings.setCompassFadeFacingNorth(false)
        }
        tapBarMenu.setOnClickListener{
            tapBarMenu.toggle()
        }
        openWallet.setOnClickListener{
            startActivity(Intent(this, Wallet::class.java))
        }
        openPopup.setOnClickListener{
            showInformationPopup()
        }
        openUserProfile.setOnClickListener{
            startActivity(Intent(this,UserProfile::class.java))
        }
        displayRates.setOnClickListener{
            AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog).apply {
                setTitle("Exc Rates For: ${sdf.format(Date())}")
                setMessage("SHIL: ${sharedPrefs.getSHIL()}\nDOLR: ${sharedPrefs.getDOLR()}\nPENY: ${sharedPrefs.getPENY()}\nQUID: ${sharedPrefs.getQUID()}\n")
                show()
            }
        }

    }

    private fun dropPins(){
        remainingCoinsAndMarkers.clear() //this is required for the case when the user launches wallet from mapbox activity, to avoid glitch with 2x the coins being rendered
        for (a in remainingCoins){ //iterate over the coins that are stored in remCoins, either from the createpins function below, or pulled from firebase
            when {
                a.currency == "PENY" ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_redmarker) //create an icon that mapbox can render based on the currency of the coin
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) } //then render the coin on the map and create a coinAndMarker object containing it

                a.currency == "SHIL" ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_bluemarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) } //markers may be tapped by user to display their value

                a.currency == "QUID" ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_yellowmarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) }

                a.currency == "DOLR" ->  {val myIcon: Icon = IconFactory.getInstance(this).fromResource(ic_greenmarker)
                    remainingCoinsAndMarkers.add(CoinAndMarker(a,map.addMarker(MarkerOptions().position(a.latLng).icon(myIcon).title("${a.currency} value : ${a.value}")))) }
            }
        }
        val myIcon = IconFactory.getInstance(this).fromResource(R.mipmap.ic_bank_icon) //drop the icon representing where the bank should be
        map.addMarker(MarkerOptions().position(LatLng(55.942963,-3.189014)).icon(myIcon).title("$$$ Central Bank $$$"))
        remainingCoins.clear() //this is fundamentally unnecessary however it keeps memory usage down and stops any issues with array synchronisation throughout the activity
        setCoins()
    }

    private fun createPins(){
        mRootRef.child("users").child(userName).child("date").setValue(sdf.format(Date())) //update firebase so that we know the user has had all 50 coins added to their account for the day
        remainingCoins.clear()
        val json = JSONObject(todayGJS) //parse the geoJSON using gson. Pull all necessary values out of the geojson, and then it won't be used again. Keep geoJSOn in sharedprefs incase another user wishes to log in.
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

    private fun checkIfNear(location:LatLng){ //this function is called in the onLocationChanged method and allows us to check every coin remaining on the maps proximity to the enw location. This could be heavily optimised however I don't think this is necessary
        var counter = remainingCoinsAndMarkers.size - 1 //use negative iteration as we are removing elements, and don't want null pointer exceptions etc
        while (counter >= 0){
            if (remainingCoinsAndMarkers[counter].coin.latLng.distanceTo(location) < distanceTo){//get the latLng of the coin on the current iteration and then compare it to the latLng passed into the function
                collectCoin(remainingCoinsAndMarkers[counter]) //use collectCoin fun to remove coin from the array;list opf coins and markers, and also remove the marker form the map. Handling of updating firebase also completed within this function
            }                                                  //arguably this creates too many updates, but on any given location change the user will probably collect at most 2 coins, and usually 0, so it's a minor inconvenience
            counter--
        }
    }

    private fun collectCoin(coinAndMarker: CoinAndMarker){
        toast("Removing marker with ID: ${coinAndMarker.coin.id}") //let the user know that they have hit a coin and that it'll be removed
        removeMarker(coinAndMarker) //see below
    }

    private fun removeMarker(coinAndMarker: CoinAndMarker){
        map.removeMarker(coinAndMarker.marker) //remove the marker held in the coinandmarker object from the map
        collectedCoins.add(coinAndMarker.coin) //add the collected coin to collectedCoins(the users wallet)
        remainingCoinsAndMarkers.remove(coinAndMarker) //remove the object from the arrayList
        setCoins() //update firebase
    }


    private fun setCoins(){
        val sharedPrefs = MySharedPrefs(this)
        if (sdf.format(Date())==sharedPrefs.getToday()){ //makesure the user hasn't tried to collect coin from yesterday, by playing at 2359 and then collecting at 0001
            setRemaining()
            setCollected()
        }else{
            toast("You don't have the current map.\nRestarting app now.")
            finishAffinity() // in this situation safest way to handle is to end all activities and let the user reload the map. Stops any issues with duplicate coins and having 100 coins on the map
        }
    }

    private fun setCollected() { //both these functions will update firebase
        val myGSON = Gson()
        val json = myGSON.toJson(collectedCoins)
        mRootRef.child("users").child(userName).child("collectedCoins").setValue(json)
    }

    private fun setRemaining(){
        remainingCoins.clear()
        for (a in remainingCoinsAndMarkers){remainingCoins.add(a.coin)} //we first must get the coins from coinsandmarkers
        val myGSON = Gson()
        val json = myGSON.toJson(remainingCoins)
        mRootRef.child("users").child(userName).child("remainingCoins").setValue(json)
        remainingCoins.clear()
    }

    private fun enableLocation(){ //below are the functions required by mapbox. The spec for them is available in the mapbox api documentation
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
        toast("This permission is required if you wish to collect any coins!")
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

    private fun showInformationPopup(){
        val builder = AlertDialog.Builder(this)
        val positiveButtonClick = { _: DialogInterface, _: Int -> }
        builder.setTitle("Information for MapBox Activity.")
        builder.setMessage("Walk around the map to collect coins! As you get within 25 metres of a coin you'll pick it up automatically.\n" +
                "\nYou can press the button at the bottom of your screen for quick access to various helpful pieces of information, as well as your wallet.\n" +
                "\nTo cash any coins in from your wallet, you must head to the central bank! Get within 50 metres to cash in your coins.")
        builder.setPositiveButton("Got it!", DialogInterface.OnClickListener(positiveButtonClick))
        builder.create()
        builder.show()
    }
}

private class CoinAndMarker(val coin:Coin, val marker:Marker) //class to hold marker and coin it represents together. Stops and issue with synchronisation