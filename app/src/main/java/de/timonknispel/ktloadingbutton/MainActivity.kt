package de.timonknispel.ktloadingbutton

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val tag = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test_button.setProgress(50F)
        test_button.setOnClickListener {
            initWithLiveData().observe(this, Observer {
                when (it) {
                    NetStatus.SUCCESS -> test_button.doResult(true) {
                        // do some other stuff when animation is done
                    }
                    NetStatus.ERROR -> test_button.doResult(false) {
                        // do some other stuff when animation is done
                    }
                    else -> test_button.startLoading()
                }
            })
        }

        // when touch is needed even if the button is in loading state (e.g. cancel loading on click)
        test_button.touchListener = {
            Log.d(tag, "button clicked")
        }
    }

    // test function for generating some data for the button
    private fun initWithLiveData(): MutableLiveData<NetStatus> {
        val liveData = MutableLiveData<NetStatus>()

        // first set state to loading
        liveData.value = NetStatus.LOADING

        // later set the result
        Handler().postDelayed({
            // post the new value
            if (Random.nextBoolean()) {
                liveData.postValue(NetStatus.SUCCESS)
            } else {
                liveData.postValue(NetStatus.ERROR)
            }
        }, Random.nextLong(2000, 4000))

        return liveData
    }
}
