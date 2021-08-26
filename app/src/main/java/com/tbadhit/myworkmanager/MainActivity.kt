package com.tbadhit.myworkmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.work.*
import com.tbadhit.myworkmanager.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

// Teori :
/*
WorkManager adalah sebuah komponen baru yang termasuk di dalam paket Jetpack.
Ia menjanjikan proses background yang pasti dieksekusi (deferrable) walaupun aplikasi ditutup
atau bahkan device di-restart. Dan yang lebih penting lagi yaitu komponen ini sudah ramah baterai,
sehingga tidak menghabiskan banyak baterai.
 */

// Codelab (One-Time Task) :
// add library "build.gradle module" (1)
// create new class "MyWorker"
// add code "MyWorker" (1)
// update code "activity_main.xml"
// add code "MainActivity" (1)
// add INTERNET PERSMISSION + usescleartraffic "AndroidManifest"

// Codelab (Periodic Task) :
// update "activity_main" (add 2 button horizontal)
// add code "MainActivity" (2)

// Codelab (Parsing JSON dengan Moshi) :
// add library moshi "build.gradle module" (2)
// create ResponseWeather (ROBO Pojo moshi) "json from https://api.openweathermap.org/data/2.5/weather?q=Jakarta&appid=23373b345890d776e2d25e7e052061c1"
// update code MyWorker (After use moshi)

class MainActivity : AppCompatActivity(), View.OnClickListener {

    // (1)
    private lateinit var binding: ActivityMainBinding
    private lateinit var workManager: WorkManager

    // (2)
    private lateinit var periodicWorkRequest: PeriodicWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // (1)
        workManager = WorkManager.getInstance(this)
        binding.btnOneTimeTask.setOnClickListener(this)
        //-----

        // (2)
        binding.btnPeriodicTask.setOnClickListener(this)
        binding.btnCancelTask.setOnClickListener(this)
        //----
    }

    // (1)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnOneTimeTask -> startOneTimeTask()
            // (2)
            R.id.btnPeriodicTask -> startPeriodicTask()
            R.id.btnCancelTask -> cancelPeriodicTask()
            //-----
        }
    }

    // (2)
    private fun startPeriodicTask() {
        binding.textStatus.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()

        // Constraint digunakan untuk memberikan syarat kapan task ini dieksekusi,
        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(constrains)
            .build()

        workManager.enqueue(periodicWorkRequest)

        // WorkInfo digunakan untuk mengetahui status task yang dieksekusi
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity, {workInfo ->
                val status = workInfo.state.name
                binding.textStatus.append("\n" + status)
                binding.btnCancelTask.isEnabled = false
                if (workInfo.state == WorkInfo.State.ENQUEUED) {
                    binding.btnCancelTask.isEnabled= true
                }
            })
    }

    // (2)
    private fun cancelPeriodicTask(){
        workManager.cancelWorkById(periodicWorkRequest.id)
    }

    // (1)
    private fun startOneTimeTask() {
        binding.textStatus.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(data)
            .build()

        workManager.enqueue(oneTimeWorkRequest)
        // WorkInfo digunakan untuk mengetahui status task yang dieksekusi
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this@MainActivity, { workInfo ->
                val status = workInfo.state.name
                binding.textStatus.append("\n" + status)
            })
    }
}