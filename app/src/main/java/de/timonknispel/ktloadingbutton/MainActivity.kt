package de.timonknispel.ktloadingbutton

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val tag = this.javaClass.simpleName
    private val handler = Handler()
    private var prog = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example for normal loading
        normal_loading_button.setOnClickListener {
            initLiveDataWithResult().observe(this, Observer { status ->
                when (status) {
                    NetStatus.LOADING -> normal_loading_button.startLoading()
                    NetStatus.SUCCESS -> normal_loading_button.doResult(true)
                    NetStatus.ERROR -> normal_loading_button.doResult(false)
                    else -> {
                    }
                }
            })
        }

        // Example for loading data with a progress
        progress_loading_button.setOnClickListener {
            initLiveDataProgress().observe(this, Observer {
                progress_loading_button.setProgress(it)
                progress_loading_percentage_text.text = "Progress: $it%"
                if (it == 100F) {
                    prog = 0F
                    handler.removeCallbacksAndMessages(null)
                    progress_loading_button.doResult(true) {
                        progress_loading_percentage_text.text = "Progress: 0%"
                    }
                }
            })
        }

        // Example for normal loading with validation
        normal_validation_loading_button.apply {
            validation = { doValidation() }
            setOnClickListener { }
        }

        // Button for resetting the validation button
        reset_normal_validation_loading_button.setOnClickListener {
            normal_validation_loading_button.reset()
        }
    }

    private fun doValidation() = normal_validation_loading_switch.isChecked

    // Function for generating live data and later return a result
    private fun initLiveDataWithResult(): MutableLiveData<NetStatus> {
        val liveData = MutableLiveData<NetStatus>()
        liveData.value = NetStatus.LOADING

        Handler().postDelayed({
            liveData.postValue(if (normal_loading_switch.isChecked) NetStatus.SUCCESS else NetStatus.ERROR)
        }, Random.nextLong(3000, 5000))

        return liveData
    }

    // test function for generating some data for the button
    private fun initLiveDataProgress(): MutableLiveData<Float> {
        val liveData = MutableLiveData<Float>()

        // first set state to loading
        liveData.value = prog

        val runnableCode = object : Runnable {
            override fun run() {
                prog += 1F
                liveData.postValue(prog)
                handler.postDelayed(this, 50)
            }
        }

        handler.post(runnableCode)
        return liveData
    }
}
