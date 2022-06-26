package com.realityexpander.security

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom

fun getHashWithSaltForPassword(password: String, saltLength: Int = 32): String {
    val salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLength)
    val saltAsHex = Hex.encodeHexString(salt)
    val hash = DigestUtils.sha256Hex("$saltAsHex$password")

    return "$saltAsHex:$hash"
}

fun isPasswordAndHashWithSaltMatching(password: String, saltHexWithHash: String): Boolean {
    val (saltAsHex, hash) = saltHexWithHash.split(":")
    val hashToCheck = DigestUtils.sha256Hex("$saltAsHex$password")

    return hashToCheck == hash
}
