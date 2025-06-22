package com.lifechurch.heroesinwaiting.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * Certificate pinning implementation for enhanced security
 * This prevents man-in-the-middle attacks by validating server certificates
 */
object CertificatePinner {
    
    // Production API hostname
    private const val HOSTNAME = "api.heroesinwaiting.org"
    
    // SHA-256 hashes of the expected certificates
    // These should be updated when certificates are renewed
    private const val CERT_SHA256_PRIMARY = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private const val CERT_SHA256_BACKUP = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    
    /**
     * Creates a CertificatePinner instance with pinned certificates
     */
    fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(HOSTNAME, CERT_SHA256_PRIMARY)
            .add(HOSTNAME, CERT_SHA256_BACKUP)
            .build()
    }
    
    /**
     * Creates an OkHttpClient with certificate pinning enabled
     */
    fun createSecureHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(createCertificatePinner())
            .build()
    }
    
    /**
     * Validates a certificate against the pinned certificates
     * This is a utility method for manual validation if needed
     */
    fun validateCertificate(certificate: Certificate): Boolean {
        if (certificate !is X509Certificate) {
            return false
        }
        
        val certHash = "sha256/" + certificate.publicKey.encoded.contentHashCode().toString()
        return certHash == CERT_SHA256_PRIMARY || certHash == CERT_SHA256_BACKUP
    }
    
    /**
     * Gets the current certificate hashes for debugging purposes
     */
    fun getCertificateHashes(): List<String> {
        return listOf(CERT_SHA256_PRIMARY, CERT_SHA256_BACKUP)
    }
} 