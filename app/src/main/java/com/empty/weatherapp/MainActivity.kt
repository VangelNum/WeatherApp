package com.empty.weatherapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.empty.weatherapp.POJO.ModelClass
import com.empty.weatherapp.Utilities.ApiUtilities
import com.empty.weatherapp.databinding.ActivityMainBinding
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        activityMainBinding.rlMainLayout.visibility = View.GONE


        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            } else false
        }


    }

    private fun getCityWeather(cityName: String) {
        val city: String = cityName.trim()
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(city, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if (response.body().toString() == "null") {
                        Toast.makeText(applicationContext,
                            "НЕДЕЙСТВИТЕЛЬНОЕ НАЗВАНИЕ ГОРОДА",
                            Toast.LENGTH_SHORT).show()
                    } else
                        setDataOnViews(response.body())
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext,
                        "НЕДЕЙСТВИТЕЛЬНОЕ НАЗВАНИЕ ГОРОДА",
                        Toast.LENGTH_SHORT)
                        .show()
                }

            })
    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                //latitude and logitude code
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "null", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "get success", Toast.LENGTH_SHORT).show()
                        //fetch weather here

                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )

                    }
                }

            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                //setting open here
            }
        } else {
            requestPermission()

            //request permission here
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object :
                Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if (response.isSuccessful) {
                        setDataOnViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                }

            })

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {
        //val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        //val currentDate = sdf.format(Date())
        activityMainBinding.tvDayMaxTemp.text =
            "Днем " + kelvinToCelsius(body!!.main.temp_max) + "°"
        activityMainBinding.tvDayMinTemp.text = "Ночью " + kelvinToCelsius(body.main.temp_min) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelsius(body.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text =
            "Ощущается как: " + kelvinToCelsius(body.main.feels_like) + "°"
        activityMainBinding.tvSunrise.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text = timeStampToLocalDate(body.sys.sunset.toLong())

        var press = body.main.pressure * 0.750062
        activityMainBinding.tvPressure.text = press.toInt().toString()

        activityMainBinding.tvHumidity.text = body.main.humidity.toString() + " %"
        activityMainBinding.tvWindSpeed.text = body.wind.speed.toString() + " м/с"
        activityMainBinding.tvTempFarenhite.text =
            "" + (kelvinToCelsius(body.main.temp).times(1.8).plus(32).roundToInt())
        activityMainBinding.etGetCityName.setText(body.name)

        when (body.weather[0].main) {
            "Clouds" -> activityMainBinding.tvWeatherType.text = "Облачно"
            "Rain" -> activityMainBinding.tvWeatherType.text = "Дождь"
            "Snow" -> activityMainBinding.tvWeatherType.text = "Снег"
            "Thunderstorm" -> activityMainBinding.tvWeatherType.text = "Гроза"
            "Drizzle" -> activityMainBinding.tvWeatherType.text = "Пасмурно"
            "Clear" -> activityMainBinding.tvWeatherType.text = "Ясно"
            "Mist" -> activityMainBinding.tvWeatherType.text = "Туман"
        }
        //activityMainBinding.tvWeatherType.text =  body.weather[0].main

        updateUI(body.weather[0].id)

    }

    private fun updateUI(id: Int) {
        val weatherView: WeatherView = findViewById(R.id.weather_change_background)
        if (id in 200..232) {
            //thunderstorm
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.aroundblack)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.aroundblack))
            /*
                activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.white_bg
                )
                activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.white_bg
                )

             */
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.black_bg)
            weatherView.setWeatherData(PrecipType.RAIN)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.thunderstorm)
        } else if (id in 300..321) {
            //drizzle
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.drizzle)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.black_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.drizzle)
            weatherView.setWeatherData(PrecipType.RAIN) // TODO
        } else if (id in 500..531) {
            //rain
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.aroundblack)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.aroundblack))

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.black_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.rain)
            weatherView.setWeatherData(PrecipType.RAIN)
        } else if (id in 600..620) {
            //snow
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.snow)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.snow_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.snow)
            weatherView.setWeatherData(PrecipType.SNOW)
        } else if (id in 701..781) {
            //mist
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.mist)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.mist))

            //activityMainBinding.ivWeatherBg.setImageResource(R.drawable.mist_bg)
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.mist_bg)
            weatherView.setWeatherData(PrecipType.CLEAR)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.mist)
        } else if (id == 800) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.clear)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))

            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.clear_bg)
            weatherView.setWeatherData(PrecipType.CLEAR)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clear)
        } else {
            //clouds
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.clouds)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))


            weatherView.setWeatherData(PrecipType.CLEAR)
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.clouds_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clouds)
        }
        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp: Long): String {
        val localTime = timeStamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .truncatedTo(ChronoUnit.MINUTES)
        }
        return localTime.toString()
    }

    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION)
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "d10492470f1619d88c88f5f4af9559e1"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }


}