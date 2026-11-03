package com.hossam.carenote.core.network

/**
 * Describes this client to the server. Provided by the app, which knows its version and device.
 *
 * The user agent names the device model, on purpose: it tells the server which phones send broken notes,
 * and it says nothing about residents.
 */
data class ClientInfo(val userAgent: String)
