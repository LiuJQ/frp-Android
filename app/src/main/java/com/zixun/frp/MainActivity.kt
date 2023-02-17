package com.zixun.frp

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val filename = "libfrpc.so"
        const val frpVer = "0.42.0"
        const val logFilename = "frpc.log"
        const val configFileName = "config.ini"
    }

    private lateinit var stateSwitch: SwitchCompat

    private lateinit var mService: ShellService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as ShellService.LocalBinder
            mService = binder.getService()
            mBound = true
            stateSwitch.isChecked = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            stateSwitch.isChecked = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "frp for Android - ${versionName}/$frpVer"

        checkConfig()
        createBGNotificationChannel()

        mBound = isServiceRunning(ShellService::class.java)
        stateSwitch = findViewById<SwitchCompat>(R.id.state_switch)
        stateSwitch.isChecked = mBound
        stateSwitch.setOnCheckedChangeListener { buttonView, isChecked -> if (isChecked) (startShell()) else (stopShell()) }
        if (mBound) {
            val intent = Intent(this, ShellService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        setListener()
    }

    private fun setListener() {
        val configButton = findViewById<Button>(R.id.configButton)
        configButton.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            //intent.putExtra("type",type+"/"+l);
            startActivity(intent)
        }

        val aboutButton = findViewById<Button>(R.id.aboutButton)
        aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }

        val refreshButton = findViewById<Button>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            CoroutineScope(context = Dispatchers.IO).launch {
                readLog(this@MainActivity)
            }
        }
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            val logfile = File(this.filesDir.toString() + "/$logFilename")
            Log.d("adx", logfile.absoluteFile.toString())
            logfile.delete()
            CoroutineScope(context = Dispatchers.IO).launch {
                readLog(this@MainActivity)
            }
        }
    }

    private suspend fun readLog(context: Context) = coroutineScope {
        val loadLog = async(Dispatchers.IO) {
            val files: Array<String> = context.fileList()
            if (files.contains(logFilename)) {
                val mReader = context.openFileInput(logFilename).bufferedReader()
                val mRespBuff = StringBuffer()
                val buff = CharArray(1024)
                var ch = 0
                while (mReader.read(buff).also { ch = it } != -1) {
                    mRespBuff.append(buff, 0, ch)
                }
                mReader.close()
                mRespBuff.toString()
            } else {
                "无日志"
            }
        }
        withContext(Dispatchers.Main) {
            val logTextView = findViewById<TextView>(R.id.logTextView)
            val logData = loadLog.await()
            logTextView.text = logData
        }
    }

    private fun checkConfig() {
        val files: Array<String> = this.fileList()
        Log.d("adx", files.joinToString(","))
        if (!files.contains(configFileName)) {
            val assetsManager = resources.assets
            this.openFileOutput(configFileName, Context.MODE_PRIVATE).use {
                it.write(assetsManager.open((configFileName)).readBytes())
            }
        }
    }

    private fun startShell() {
        val intent = Intent(this, ShellService::class.java)
        intent.putExtra("filename", filename)
        startService(intent)
        // Bind to LocalService
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun stopShell() {
        val intent = Intent(this, ShellService::class.java)
        unbindService(connection)
        stopService(intent)
    }

    private fun createBGNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "frp后台服务"
            val descriptionText = "frp后台服务通知"
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel("shell_bg", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}