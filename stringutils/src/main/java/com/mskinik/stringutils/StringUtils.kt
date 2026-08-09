package com.mskinik.stringutils

import android.util.Log
import com.mskinik.utils.model.City
import com.mskinik.utils.model.PackageClass
import com.mskinik.utils.model.Person

object StringUtils {
    // BREAKING CHANGE (v4.0.0): renamed from reverse(text) -> reverseText(text)
    // to make the method name consistent with the other *Text functions below.
    // See :rewrite-recipes module for the OpenRewrite recipe that automates
    // this migration for consumers.
    fun reverseText(text: String): String = text.reversed()

    fun capitalizeWords(text: String): String =
        text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    fun removeTurkishChars(text: String): String {
        val turkishMap = mapOf(
            'ç' to 'c', 'Ç' to 'C',
            'ğ' to 'g', 'Ğ' to 'G',
            'ı' to 'i', 'İ' to 'I',
            'ö' to 'o', 'Ö' to 'O',
            'ş' to 's', 'Ş' to 'S',
            'ü' to 'u', 'Ü' to 'U'
        )
        return text.map { turkishMap[it] ?: it }.joinToString("")
    }

    fun newfun() = "newfun"

    fun tagv140funtion() = "v1.4.0"

    fun tryModelLibrary(){
        val person: Person = Person(name = "James")
    }

    fun tryModelLibraryPackageClass(){
       val packageClass =  PackageClass()
        Log.d("TAG", "tryModelLibraryPackageClass: girdi1 packageName = ${packageClass.packageName} appName = ${packageClass.appName} appVersion = ${packageClass.appVersion}")
    }
    fun tryModelLibraryCityClass(){
       val city =  City(name = "Bursa","Marmara")
        Log.d("TAG", "tryModelLibraryCityClass: girdi1 cityName = ${city.name} cityRegion = ${city.region} ")

    }

}
