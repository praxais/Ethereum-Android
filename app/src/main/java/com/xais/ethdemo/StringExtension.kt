package com.xais.ethdemo

/**
 * Created by prajwal on 3/25/19.
 */

fun String.addString(name: String): String {
    if (this.isEmpty()) return name
    return "$this $name"
}