package com.badoo.ribs.sandbox.rib.foo_bar

import android.view.ViewGroup
import com.badoo.ribs.clienthelper.connector.Connectable
import com.badoo.ribs.clienthelper.connector.NodeConnector
import com.badoo.ribs.core.Node
import com.badoo.ribs.core.builder.BuildParams
import com.badoo.ribs.core.plugin.Plugin
import com.badoo.ribs.sandbox.rib.foo_bar.FooBar.Input
import com.badoo.ribs.sandbox.rib.foo_bar.FooBar.Output

class FooBarNode(
    buildParams: BuildParams<*>,
    viewFactory: ((ViewGroup) -> FooBarView?)?,
    plugins: List<Plugin> = emptyList(),
    connector: NodeConnector<Input, Output> = NodeConnector()
) : Node<FooBarView>(
    buildParams = buildParams,
    viewFactory = viewFactory,
    plugins = plugins
), FooBar, Connectable<Input, Output> by connector {

}
