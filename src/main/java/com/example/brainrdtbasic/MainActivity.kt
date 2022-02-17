package com.example.brainrdtbasic

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.lang.NullPointerException

class MainActivity : AppCompatActivity() {
    @JvmField
    var videolist: String? = null
    private var mFragment: Fragment? = null
    val VIDEO_LIST_KEY:String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Receive mess from other activity
        var tag: String? = ""
        tag = intent.getStringExtra("fragment");
        videolist = intent.getStringExtra("videolist");

        when (tag) {
            "6" -> {
                displayView(6)
            }
            "4" ->{
                if (savedInstanceState == null) {
                    displayView(4)
                }
            }
            else -> displayView(6)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState, outPersistentState)

        outState?.run {
            putString(VIDEO_LIST_KEY, videolist)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        videolist = savedInstanceState?.getString(VIDEO_LIST_KEY)
    }

    override fun onStop() {

        super.onStop()

    }
    fun displayView(position: Int) {
        mFragment = null
        mFragment = when (position) {
            else -> MainFragment.newInstance()
        }
        if (mFragment != null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.replace(R.id.nav_host_fragment_content_main, mFragment!!)
            fragmentTransaction.commit()
        } else {
            Log.e("MainActivity", "Error in creating fragment")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (mFragment != null) {
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().remove(mFragment!!).commit()
        }
        if (supportFragmentManager.backStackEntryCount < 3) {
            finish()
        } else {
            super.onBackPressed()
        }
        super.onBackPressed()
    }

}