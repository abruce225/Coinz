package uk.ac.ed.inf.alexyz.coinz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView

import kotlinx.android.synthetic.main.activity_map_box_main.*

class MapBoxMain : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_box_main)
        setSupportActionBar(toolbar)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)


    }

}
