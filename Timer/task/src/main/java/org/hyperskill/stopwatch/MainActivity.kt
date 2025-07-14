package org.hyperskill.stopwatch

import android.Manifest
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private val timerDisplay: TextView by lazy { findViewById(R.id.textView) }
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
    private val settingsButton: Button by lazy { findViewById(R.id.settingsButton) }

    private var minutes = 0
    private var seconds = 0
    private var isActive = false
    private var upperLimit: Int? = null  // in seconds
    private val barColors = listOf(0xFF66558E, 0xFF416835, 0xFF6D5E0F)
    private var currentColor = 0
    private val handler = Handler(Looper.getMainLooper())

    private var CHANNEL_ID = "org.hyperskill"
    private var NOTIFICATION_ID = 393939
    private var REQUEST_CODE_NOTIFICATION = 1001

    private val updateTimer = object : Runnable {
        override fun run() {
            seconds++
            if (seconds == 60) {
                minutes++
                seconds = 0
            }

            val totalTime = minutes * 60 + seconds
            if (upperLimit != null) {
                if (totalTime == upperLimit) {
                    showNotificationIfNeeded()  // 🔔 show notification once
                    timerDisplay.setTextColor(Color.RED)
                } else if (totalTime > upperLimit!!) {
                    timerDisplay.setTextColor(Color.RED)
                } else {
                    timerDisplay.setTextColor(Color.BLACK)
                }
            }


            updateText()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        updateText()

        findViewById<Button>(R.id.startButton).setOnClickListener { startTimer() }
        findViewById<Button>(R.id.startButton).setOnClickListener{ handleStartButton() }
        findViewById<Button>(R.id.resetButton).setOnClickListener { stopTimer() }

        settingsButton.setOnClickListener {
            if (isActive) return@setOnClickListener

            }

        val inputView = layoutInflater.inflate(R.layout.dialog_input, null)
        val input = inputView.findViewById<EditText>(R.id.upperLimitEditText)

        AlertDialog.Builder(this)
            .setTitle("Set upper limit in seconds")
            .setView(inputView)
            .setPositiveButton("OK") { dialog, _ ->
                val text = input.text.toString()
                val newLimit = text.toIntOrNull()
                if (newLimit != null) {
                    upperLimit = newLimit
                    Toast.makeText(this, "Upper limit set to $upperLimit seconds", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun handleStartButton() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
                startTimer()
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
            }
        }else {
            startTimer()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTimer()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startTimer() {
        if (isActive) return

        isActive = true
        settingsButton.isEnabled = false  // Disable settings during timer
        currentColor = 0  // Reset to known color on start

        handler.postDelayed(updateTimer, 1000)

        progressBar.apply {
            visibility = View.VISIBLE
            indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
        }
    }

    private fun stopTimer() {
        handler.removeCallbacks(updateTimer)
        minutes = 0
        seconds = 0
        isActive = false

        timerDisplay.setTextColor(Color.BLACK)
        progressBar.visibility = View.INVISIBLE
        settingsButton.isEnabled = true  // Re-enable settings after stopping

        updateText()
    }

    private fun updateText() {
        timerDisplay.text = String.format("%02d:%02d", minutes, seconds)

        // Rotate to a new color
        val previousColor = currentColor
        do {
            currentColor = (currentColor + 1) % barColors.size
        } while (currentColor == previousColor)

        progressBar.indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stopwatch Limit Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotificationIfNeeded() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Only show once
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                // safe to call on API 26+
                createNotificationChannel()
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Use your icon
            .setContentTitle("Stopwatch Limit Reached")
            .setContentText("The timer exceeded your upper limit.")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}






//class MainActivity : AppCompatActivity() {
//    private val timerDisplay: TextView by lazy { findViewById(R.id.textView) }
//    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
//    private var minutes = 0
//    private var seconds = 0
//    private var isActive = false
//    private var upperLimit: Int? = null
//    private val barColors = listOf(0xFF66558E, 0xFF416835, 0xFF6D5E0F)
//    private var currentColor = 0
//    private val handler = Handler(Looper.getMainLooper())
//    private val updateTimer = object: Runnable {
//        override fun run() {
//            seconds++
//
//            if (seconds == 60) {
//                minutes++
//                seconds = 0
//            }
//
//            // If upperLimit is set and reached, change color to RED
//            val totalSeconds = minutes * 60 + seconds
//            if (upperLimit != null && totalSeconds >= upperLimit!!) {
//                progressBar.indeterminateTintList = ColorStateList.valueOf(Color.RED)
//            } else {
//                currentColor = (currentColor + 1) % barColors.size
//                progressBar.indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
//            }
//
//            updateText()
//            handler.postDelayed(this,1000)
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        updateText()
//
//        findViewById<Button>(R.id.startButton).setOnClickListener { startTimer() }
//        findViewById<Button>(R.id.resetButton).setOnClickListener { stopTimer() }
//
//        val settingsButton = findViewById<Button>(R.id.settingsButton)
//        settingsButton.setOnClickListener {
//            if (isActive) return@setOnClickListener // don't allow settings while running
//
//            val inputField = EditText(this)
//            inputField.inputType = android.text.InputType.TYPE_CLASS_NUMBER
//
//            val dialog = AlertDialog.Builder(this)
//                .setTitle("Set Upper Limit")
//                .setView(inputField)
//                .setPositiveButton("OK") { dialog, _ ->
//                    val input = inputField.text.toString()
//                    if (input.isNotBlank() && input.toIntOrNull() != null) {
//                        upperLimit = input.toInt()
//                        Toast.makeText(this, "Upper limit set to $upperLimit seconds", Toast.LENGTH_SHORT).show()
//                    } else {
//                        upperLimit = null
//                        Toast.makeText(this, "Invalid input. Limit not set.", Toast.LENGTH_SHORT).show()
//                    }
//                    dialog.dismiss()
//                }
//                .setNegativeButton("Cancel") { dialog, _ ->
//                    dialog.cancel()
//                }
//                .create()
//
//            dialog.show()
//        }
//    }
//
//    private fun startTimer() {
//        if (isActive) return
//
//        isActive = true
//        handler.postDelayed(updateTimer, 1000)
//        progressBar.apply {
//            visibility = VISIBLE
//            indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
//        }
//    }
//
//    private fun stopTimer() {
//        handler.removeCallbacks(updateTimer)
//        minutes = 0
//        seconds = 0
//        progressBar.visibility = INVISIBLE
//        isActive = false
//        updateText()
//    }
//
//    private fun updateText() {
//        timerDisplay.text = getText(R.string.timer).toString().format(minutes, seconds)
//
//        currentColor = (currentColor + 1) % barColors.size
//        progressBar.indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
//    }
//
//
//
//
//}
//    private var seconds = 0
//    private var isRunning = false
//    private lateinit var handler: Handler
//    private var countDownTask: Runnable? = null
//    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
//    private val barColors = listOf(0xFF66558E, 0xFF416835, 0xFF6D5E0F)
//    private var currentColor = 0
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        val textView = findViewById<TextView>(R.id.textView)
//        val startButton = findViewById<Button>(R.id.startButton)
//        val resetButton = findViewById<Button>(R.id.resetButton)
//        //val progressBar = findViewById<ProgressBar>(R.id.progressBar)
//
////        val colors = listOf(Color.RED, Color.BLUE)
////        var colorIndex = 0
//        var colorJob: Job? = null
//
//        handler = Handler(Looper.getMainLooper())
//
//        startButton.setOnClickListener {
//            if (!isRunning) {
//                isRunning = true
//                startButton.isEnabled = false
//
//                countDownTask = object : Runnable {
//                    override fun run() {
//                        if (seconds < 1500) {
//                            val minutes = seconds / 60
//                            val remainingSeconds = seconds % 60
//                            textView.text = String.format("%02d:%02d", minutes, remainingSeconds)
//                            progressBar.indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
//                            seconds++
//                            handler.postDelayed(this, 1000)
//                        } else {
//                            isRunning = false
//                            startButton.isEnabled = true
//                        }
//                    }
//                }
//
//                handler.post(countDownTask!!)
//            }
//            progressBar.apply {
//                visibility = View.VISIBLE
//                indeterminateTintList = ColorStateList.valueOf(barColors[currentColor].toInt())
//            }
////            progressBar.visibility = View.VISIBLE
////            colorJob = lifecycleScope.launch {
////                while (isActive) {
////                    val color = Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
////                    progressBar.indeterminateTintList = ColorStateList.valueOf(color)
////                    delay(1000)
////                }
////            }
//
//        }
//
//        resetButton.setOnClickListener {
//            countDownTask?.let { handler.removeCallbacks(it) } // Safely remove runnable
//            countDownTask = null // Null it out to ensure it's fresh next time
//            seconds = 0
//            isRunning = false
//            textView.text = String.format("%02d:%02d", 0, 0)
//            startButton.isEnabled = true
//
//            progressBar.visibility = View.INVISIBLE
////            colorJob?.cancel()
////            colorJob = null
//        }
//
//
//
//
//
//
//
//
////        handler.postAtTime(updateLight, SystemClock.uptimeMillis() + nextTime)
//
//        /*
//            Tests for android can not guarantee the correctness of solutions that make use of
//            mutation on "static" variables to keep state. You should avoid using those.
//            Consider "static" as being anything on kotlin that is transpiled to java
//            into a static variable. That includes global variables and variables inside
//            singletons declared with keyword object, including companion object.
//            This limitation is related to the use of JUnit on tests. JUnit re-instantiate all
//            instance variable for each test method, but it does not re-instantiate static variables.
//            The use of static variable to hold state can lead to state from one test to spill over
//            to another test and cause unexpected results.
//            Using mutation on static variables to keep state
//            is considered a bad practice anyway and no measure
//            attempting to give support to that pattern will be made.
//         */
//    }
//}