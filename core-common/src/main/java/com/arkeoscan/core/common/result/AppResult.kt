package com.arkeoscan.core.common.result

/**
 * Repository ve UseCase katmanlarında kullanılan ortak sonuç sarmalayıcısı.
 * Exception fırlatmak yerine açık başarı/hata durumu taşır.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    fun getOrNull(): T? = (this as? Success)?.data

    companion object {
        fun <T> of(block: () -> T): AppResult<T> = try {
            Success(block())
        } catch (e: Exception) {
            Error(e.message ?: "Bilinmeyen hata", e)
        }
    }
}
