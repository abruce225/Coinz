package uk.ac.ed.inf.alexyz.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

//This is a simple object, designed to hold all necessary info about a given coin. Date field allows checking of expiration on coins

class Coin(val id:String,val currency:String,val value:Double,val latLng: LatLng,val date:String)