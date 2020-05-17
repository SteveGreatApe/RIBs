package com.badoo.ribs.core

import com.badoo.ribs.core.plugin.NodeLifecycleAware
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NodePluginNodeLifecycleAwareTest : NodePluginTest() {

    @Test
    fun `NodeLifecycleAware plugins receive onAttach()`() {
        val (node, plugins) = testPlugins<NodeLifecycleAware>()

        node.onAttach()

        plugins.forEach {
            verify(it).onAttach(eq(node.lifecycleManager.ribLifecycle.lifecycle))
        }
    }

    @Test
    fun `NodeLifecycleAware plugins receive onDetach()`() {
        val (node, plugins) = testPlugins<NodeLifecycleAware>()

        node.onDetach()

        plugins.forEach {
            verify(it).onDetach()
        }
    }
}