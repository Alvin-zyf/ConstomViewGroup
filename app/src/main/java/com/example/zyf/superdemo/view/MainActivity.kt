package com.example.zyf.superdemo.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.zyf.superdemo.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var index = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn2.setOnClickListener {
            index++

            if(index >= 3){
                index = 0
            }

            when (index) {
                0 -> {
                    leadView.mHintView = img1
                    leadView.mShape = LeadLayout.OVAL
                }

                1 -> {
                    leadView.mHintView = img2
                    leadView.mShape = LeadLayout.CIRCLE
                }

                2 -> {
                    leadView.mHintView = img3
                    leadView.mShape = LeadLayout.SQUARE
                }
            }

        }
    }
}
