package com.tbadhit.myworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tbadhit.myworkmanager.model.ResponseWeather
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.text.DecimalFormat

// (1) extend worker & add property
class MyWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

    // (1)
    // resultStatus = kita bisa menentukan kembalian dari proses ini apakah berhasil atau gagal dengan menggunakan kode tersebut.
    private var resultStatus: Result? = null

    // (1)
    // (doWork) = adalah metode yang akan dipanggil ketika WorkManager berjalan
    override fun doWork(): Result {
        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrentWeather(dataCity)
    }

    // (1)
    private fun getCurrentWeather(city: String?): Result {
        Log.d(TAG, "getCurrentWeather: Mulai..")
        Looper.prepare()
        val client = SyncHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$APP_ID"
        Log.d(TAG, "getCurrentWeather: $url")
        client.post(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray
            ) {
                val result = String(responseBody)
                Log.d(TAG, result)
                try {

                    // Before User Moshi :
                    // val responseObject = JSONObject(result)
                    // val currentWeather: String = responseObject.getJSONArray("weather").getJSONObject(0).getString("main")
                    // val description: String = responseObject.getJSONArray("weather").getJSONObject(0).getString("description")
                    // val tempInKelvin = responseObject.getJSONObject("main").getDouble("temp")

                    // (After Use Moshi)
                    val moshi = Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()

                    val jsonAdapter = moshi.adapter(ResponseWeather::class.java)
                    val response = jsonAdapter.fromJson(result)

                    response?.let {
                        val currentWeather = it.weather?.get(0)?.main
                        val description = it.weather?.get(0)?.description
                        val tempInKelvin = it.main?.temp
                        val tempInCelsius = tempInKelvin?.minus(273)
                        val temperature: String = DecimalFormat("##.##").format(tempInCelsius)
                        val title = "Current Weather in $city"
                        val message = "$currentWeather, $description with $temperature celsius"
                        showNotification(title, message)
                    }
                    //-------------------------

                    Log.d(TAG, "onSuccess: Selesai.....")
                    resultStatus = Result.success()

                } catch (e: Exception) {
                    showNotification("Get Current Weather Not Success", e.message)
                    Log.d(TAG, "onSuccess: Gagal.....")
                    resultStatus = Result.failure()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d(TAG, "onFailure: Gagal.....")
                // ketika proses gagal, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                showNotification("Get Current Weather Failed", error?.message)
                resultStatus = Result.failure()
            }

        })

        return resultStatus as Result
    }

    // (1)
    private fun showNotification(title: String, description: String?) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    // (1)
    companion object {
        private val TAG = MyWorker::class.java.simpleName
        const val APP_ID = "23373b345890d776e2d25e7e052061c1"
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "adhit channel"
    }
}