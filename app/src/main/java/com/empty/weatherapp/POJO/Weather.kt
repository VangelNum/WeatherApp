package com.empty.weatherapp.POJO

import com.google.gson.annotations.SerializedName

data class Weather (
    @SerializedName("id") val id:Int,
    @SerializedName("main") val main:String,
    @SerializedName("descriptor") val descriptor:String,
    @SerializedName("icon") val icon:String
)