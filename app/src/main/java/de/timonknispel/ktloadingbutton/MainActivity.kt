package de.timonknispel.ktloadingbutton

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val tag = this.javaClass.simpleName

    val handler = Handler()

    var prog = 0F
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initWithLiveData().observe(this, Observer {
            test_button.setProgress(it)
            if (it == 100F) {
                handler.removeCallbacksAndMessages(null)
                test_button.doResult(true)
            }
        })

        // when touch is needed even if the button is in loading state (e.g. cancel loading on click)
        test_button.touchListener = {
            Log.d(tag, "button clicked")
        }
    }

    // test function for generating some data for the button
    private fun initWithLiveData(): MutableLiveData<Float> {
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
