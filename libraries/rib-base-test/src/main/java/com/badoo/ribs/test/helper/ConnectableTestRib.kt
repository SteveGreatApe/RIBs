package com.badoo.ribs.test.helper

import com.badoo.ribs.clienthelper.connector.Connectable
import com.badoo.ribs.test.helper.ConnectableTestRib.Input
import com.badoo.ribs.test.helper.ConnectableTestRib.Output

interface ConnectableTestRib : Connectable<Input, Output> {

    sealed class Input

    sealed class Output {
        object Output1 : Output()
        object Output2 : Output()
        object Output3 : Output()
    }
}
