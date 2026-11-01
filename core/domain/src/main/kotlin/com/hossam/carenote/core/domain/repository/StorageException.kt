package com.hossam.carenote.core.domain.repository

/**
 * The phone's own storage failed, e.g. the disk is full. The data layer turns its database errors into
 * this, so the domain can react without knowing about SQLite; programming errors are never wrapped.
 */
class StorageException(cause: Throwable) : Exception(cause)
