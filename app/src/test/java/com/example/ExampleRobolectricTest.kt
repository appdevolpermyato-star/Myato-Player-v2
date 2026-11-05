package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Myato Player", appName)
  }

  @Test
  fun `floating player prefs defaults`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(100, com.example.service.FloatingPlayerPrefs.getPanelSizePercent(context))
    assertEquals(100, com.example.service.FloatingPlayerPrefs.getPanelOpacityPercent(context))
  }
}
