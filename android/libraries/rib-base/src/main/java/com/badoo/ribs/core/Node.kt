/*
 * Copyright (C) 2017. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badoo.ribs.core

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.CallSuper
import android.support.annotation.MainThread
import android.support.annotation.VisibleForTesting
import android.util.SparseArray
import android.view.ViewGroup
import com.badoo.ribs.core.view.RibView
import com.badoo.ribs.core.view.ViewFactory
import com.badoo.ribs.core.requestcode.RequestCodeRegistry
import com.uber.rib.util.RibRefWatcher
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Responsible for handling the addition and removal of child nodes.
 **/
open class Node<V : RibView>(
    val forClass: Class<*>,
    private val viewFactory: ViewFactory<V>?,
    private val router: Router<*, V>,
    private val interactor: Interactor<*, V>,
    private val ribRefWatcher: RibRefWatcher = RibRefWatcher.getInstance()
) {
    companion object {
        internal const val KEY_CHILD_NODES = "node.children"
        internal const val KEY_ROUTER = "node.router"
        internal const val KEY_INTERACTOR = "node.interactor"
        private const val KEY_RIB_ID = "rib.id"
        private const val KEY_VIEW_STATE = "view.state"
        private val requestCodeRegistry = RequestCodeRegistry(8)
    }

    init {
        router.node = this
    }

    private var savedInstanceState: Bundle? = null
    internal val children = CopyOnWriteArrayList<Node<*>>()
    protected var tag: String = "${this::class.java.name}.${UUID.randomUUID()}"
        private set
    private var ribId: Int? = null
    internal var view: V? = null
        private set
    protected var parentViewGroup: ViewGroup? = null

    private var savedViewState: SparseArray<Parcelable> = SparseArray()


    private fun generateRibId(): Int =
        requestCodeRegistry.generateGroupId(tag)

    private fun generateRequestCode(code: Int): Int =
        requestCodeRegistry.generateRequestCode(tag, code)

    private fun updateRibId(value: Int) {
        ribId = value
    }

    fun attachToView(parentViewGroup: ViewGroup) {
        this.parentViewGroup = parentViewGroup
        view = createView(parentViewGroup)
        view?.let {
            parentViewGroup.addView(it.androidView) // todo test invoked
            it.androidView.restoreHierarchyState(savedViewState) // todo test invoked
            interactor.onViewCreated(it) // todo test invoked
        }

        println("before children.forEach")
        children.forEach {
            attachChildView(it)
        }
    }

    private fun createView(parentViewGroup: ViewGroup): V? =
        viewFactory?.invoke(parentViewGroup) // todo test invoked

    internal fun attachChild(child: Node<*>, bundle: Bundle? = null) {
        attachChildView(child)
        attachChildNode(child, bundle)
    }

    internal fun attachChildView(child: Node<*>) {
        // parentViewGroup is guaranteed to be non-null if and only if Android view system is available
        parentViewGroup?.let {
            println("before children.forEach")
            child.attachToView(
                router.getParentViewForChild(child.forClass, view) ?: it
            )
        }
    }

    internal fun detachChild(child: Node<*>) {
        detachChildNode(child)
        detachChildView(child)
    }

    internal fun saveViewState() {
        view?.let {
            it.androidView.saveHierarchyState(savedViewState)
        }
    }

    internal fun detachChildView(child: Node<*>) {
        parentViewGroup?.let {
            child.onDetachFromView(
                parentViewGroup = router.getParentViewForChild(child.forClass, view) ?: it
            )
        }
    }

    fun onDetachFromView(parentViewGroup: ViewGroup) {
        children.forEach {
            detachChildView(it)
        }

        view?.let {
            parentViewGroup.removeView(it.androidView)
            interactor.onViewDestroyed()
        }

        view = null
        this.parentViewGroup = null
    }

    /**
     * Dispatch back press to the associated interactor.
     *
     * @return TRUE if the interactor handled the back press and no further action is necessary.
     */
    @CallSuper
    open fun handleBackPress(): Boolean {
        ribRefWatcher.logBreadcrumb("BACKPRESS", null, null)
        return router.popBackStack() || interactor.handleBackPress()
    }

    /**
     * Called when a node is being attached. Node subclasses can perform setup here for anything
     * that is needed again but is cleaned up in willDetach(). Use didLoad() if the setup is only
     * needed once.
     */
    protected fun willAttach() {}

    /**
     * Called when a node is being a detached, node subclasses should perform any required clean
     * up here.
     */
    protected fun willDetach() {}

    /**
     * Attaches a child node to this node.
     *
     * @param childNode the [Node] to be attached.
     */
    @MainThread
    protected fun attachChildNode(childNode: Node<*>, bundle: Bundle?) {
        children.add(childNode)
        ribRefWatcher.logBreadcrumb(
            "ATTACHED", childNode.javaClass.simpleName, this.javaClass.simpleName
        )

        childNode.dispatchAttach(bundle)
    }

    /**
     * Detaches the node from this parent. NOTE: No consumers of
     * this API should ever keep a reference to the detached child, leak canary will enforce
     * that it gets garbage collected.
     *
     * @param childNode the [Node] to be detached.
     */
    @MainThread
    protected fun detachChildNode(childNode: Node<*>) {
        children.remove(childNode)

        val interactor = childNode.interactor
        ribRefWatcher.watchDeletedObject(interactor)
        ribRefWatcher.logBreadcrumb(
            "DETACHED", childNode.javaClass.simpleName, this.javaClass.simpleName
        )

        childNode.dispatchDetach()
    }

    @CallSuper
    open fun dispatchAttach(savedInstanceState: Bundle?) {
        this.savedInstanceState = savedInstanceState

        updateRibId(savedInstanceState?.getInt(KEY_RIB_ID, generateRibId()) ?: generateRibId())
        savedViewState = savedInstanceState?.getSparseParcelableArray<Parcelable>(KEY_VIEW_STATE) ?: SparseArray()

        willAttach()

        router.dispatchAttach(savedInstanceState?.getBundle(KEY_ROUTER))
        interactor.dispatchAttach(savedInstanceState?.getBundle(KEY_INTERACTOR))
    }

    open fun dispatchDetach() {
        interactor.dispatchDetach()
        router.dispatchDetach()
        willDetach()

        for (child in children) {
            detachChildNode(child)
        }
    }

    open fun saveInstanceState(outState: Bundle) {
        outState.putInt(KEY_RIB_ID, ribId ?: generateRibId().also { updateRibId(it) })
        outState.putSparseParcelableArray(KEY_VIEW_STATE, savedViewState)
        saveRouterState(outState)
        saveInteractorState(outState)
    }

    private fun saveRouterState(outState: Bundle) {
        Bundle().let {
            router.onSaveInstanceState(it)
            outState.putBundle(KEY_ROUTER, it)
        }
    }

    private fun saveInteractorState(outState: Bundle) {
        Bundle().let {
            interactor.onSaveInstanceState(it)
            outState.putBundle(KEY_INTERACTOR, it)
        }
    }

    fun onStart() {
        interactor.onStart()
        children.forEach { it.onStart() }
    }

    fun onStop() {
        interactor.onStop()
        children.forEach { it.onStop() }
    }

    fun onResume() {
        interactor.onResume()
        children.forEach { it.onResume() }
    }

    fun onPause() {
        interactor.onPause()
        children.forEach { it.onPause() }
    }

    override fun toString(): String =
        "Node@${hashCode()} (${forClass.simpleName})"
}

