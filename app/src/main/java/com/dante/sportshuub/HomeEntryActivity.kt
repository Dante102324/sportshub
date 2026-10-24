package com.dante.sportshuub

import android.app.Activity
import android.content.Intent
import android.os.Bundle

// The "home" door. The system opens this when the device boots or the Home
// button is pressed (once Sports Hub is the default home app). It has no screen:
// it sends you to Sports Hub, or to the TV's own home menu after "Exit".
class HomeEntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sendToTvMenu = LauncherMode.isExited(this) && LauncherMode.openSystemHome(this)

        if (!sendToTvMenu) {
            // Either we're in frontend mode, or there is no TV menu to go to.
            LauncherMode.clearExited(this)

            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }

        finish()
    }
}
