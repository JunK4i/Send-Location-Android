package com.example.sendlocationdata

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.spec.GCMParameterSpec

object AESUtils {
    private const val AES = "AES/CBC/PKCS5Padding"

    fun encryptData(data: String, key: ByteArray, iv: ByteArray): ByteArray {
        val secretKeySpec = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmParameterSpec = GCMParameterSpec(128, iv) // 128 bit authenticator tag length
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec)
        return  cipher.doFinal(data.toByteArray(Charsets.UTF_8))

    }

    fun generateIV(): ByteArray {
        val secureRandom = SecureRandom()
        val iv = ByteArray(12) // 96 bits for GCM, might be 16 bytes (128 bits) for CBC
        secureRandom.nextBytes(iv)
        return iv
    }



}
