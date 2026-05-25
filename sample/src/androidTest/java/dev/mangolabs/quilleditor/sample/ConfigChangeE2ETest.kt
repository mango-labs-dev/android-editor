package dev.mangolabs.quilleditor.sample

import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mangolabs.quilleditor.model.Delta
import dev.mangolabs.quilleditor.model.DeltaOp
import dev.mangolabs.quilleditor.model.textInsert
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Activity recreation (rotation, dark-mode flip, font-scale change) drops the
 * WebView and the QuillState along with it. The Activity-scoped ViewModel
 * survives across the recreation, so [SampleEditorScreen] can hydrate a
 * fresh QuillState from [SampleViewModel.savedDelta] on the new Activity
 * instance — this test verifies the VM survival contract that the e2e
 * persistence flow depends on.
 */
@RunWith(AndroidJUnit4::class)
class ConfigChangeE2ETest {

  @get:Rule
  val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @Test
  fun savedDeltaSurvivesActivityRecreation() {
    val saved = Delta(
      listOf(DeltaOp(insert = JsonPrimitive("survives recreation")))
    )

    activityRule.scenario.onActivity { activity ->
      ViewModelProvider(activity)[SampleViewModel::class.java].setSavedDelta(saved)
    }

    activityRule.scenario.recreate()

    activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[SampleViewModel::class.java]
      val restored = vm.savedDelta.value
      assertNotNull("VM should retain saved delta after Activity recreation", restored)
      assertTrue(
        "restored Delta should contain the original text",
        restored!!.ops.any { it.textInsert?.contains("survives recreation") == true }
      )
    }
  }
}
