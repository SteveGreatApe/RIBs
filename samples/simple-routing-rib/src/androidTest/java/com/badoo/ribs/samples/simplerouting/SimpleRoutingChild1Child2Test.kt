package com.badoo.ribs.samples.simplerouting

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.badoo.ribs.test.RibsRule
import com.badoo.ribs.core.modality.BuildContext.Companion.root
import com.badoo.ribs.samples.simplerouting.rib.R
import com.badoo.ribs.samples.simplerouting.rib.simple_routing_child1_child2.SimpleRoutingChild1Child2
import com.badoo.ribs.samples.simplerouting.rib.simple_routing_child1_child2.builder.SimpleRoutingChild1Child2Builder
import org.junit.Rule
import org.junit.Test

class SimpleRoutingChild1Child2Test {

    @get:Rule
    val ribsRule = RibsRule { _, savedInstanceState -> buildRib(savedInstanceState) }

    private fun buildRib(savedInstanceState: Bundle?) =
        SimpleRoutingChild1Child2Builder(
                object : SimpleRoutingChild1Child2.Dependency {}
        ).build(root(savedInstanceState))

    @Test
    fun showsTitleText() {
        onView(withId(R.id.child1_child2_title))
            .check(matches(withText(R.string.child1_child2_text)))
    }
}
