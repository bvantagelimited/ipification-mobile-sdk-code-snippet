package com.ipification.im.utils

import android.util.Log
import androidx.annotation.Keep
import java.text.SimpleDateFormat
import java.util.*

internal class LogUtils {
    /*
    * Maintain which log level the user has allowed to logged by
    * adding all the allowed log levels to a set.
    * */
    @Keep
    companion object {
        private val logLevels = mutableSetOf<LogLevel>()
        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS")

            return sdf.format(Date())
        }
        fun addLevel(logLevel: LogLevel) {
            if (logLevel == LogLevel.ALL) {
                logLevels.add(
                    LogLevel.DEBUG
                )
                logLevels.add(
                    LogLevel.INFO
                )
                logLevels.add(
                    LogLevel.WARN
                )
                logLevels.add(
                    LogLevel.VERBOSE
                )
                logLevels.add(
                    LogLevel.ERROR
                )
                logLevels.add(
                    LogLevel.WTF
                )
            } else {
                logLevels.add(logLevel)
            }
        }

        fun addLevel(vararg logLevel: LogLevel) {
            if (logLevel.contains(LogLevel.ALL)) {
                logLevels.add(
                    LogLevel.DEBUG
                )
                logLevels.add(
                    LogLevel.INFO
                )
                logLevels.add(
                    LogLevel.WARN
                )
                logLevels.add(
                    LogLevel.VERBOSE
                )
                logLevels.add(
                    LogLevel.ERROR
                )
                logLevels.add(
                    LogLevel.WTF
                )
            } else {
                logLevels.addAll(logLevel)
            }
        }

        fun removeLevel(logLevel: LogLevel) {
            logLevels.remove(logLevel)
        }

        fun containsLevel(logLevel: LogLevel): Boolean = logLevel in logLevels
    }
}

//Different log levels that user can select
enum class LogLevel {
    ALL,
    INFO,
    DEBUG,
    WARN,
    VERBOSE,
    WTF,
    ERROR,
}

fun Any.info(message: String) {
    if (LogUtils.containsLevel(
            LogLevel.INFO
        )
    ) {
        Log.i(javaClass.simpleName, message)
    }
}

fun Any.debug(message: String) {
    if (LogUtils.containsLevel(
            LogLevel.DEBUG
        )
    ) {
        Log.d(javaClass.simpleName, message)
    }
}

fun Any.error(message: String) {
    if (LogUtils.containsLevel(
            LogLevel.ERROR
        )
    ) {
        Log.e(javaClass.simpleName, message)
    }
}

fun Any.verbose(message: String) {
    if (LogUtils.containsLevel(
            LogLevel.VERBOSE
        )
    ) {
        Log.v(javaClass.simpleName, message)
    }
}

fun Any.warn(message: String) {
    if (LogUtils.containsLevel(
            LogLevel.WARN
        )
    ) {
        Log.w(javaClass.simpleName, message)
    }
}

fun Any.wtf(message: String) {
    if (LogUtils.containsLevel(
            LogLevel.WTF
        )
    ) {
        Log.wtf(javaClass.simpleName, message)
    }
}