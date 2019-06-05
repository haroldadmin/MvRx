package com.airbnb.mvrx.benchmark

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class RegularStateStore<S : Any>(initialState: S) : MvRxStateStore<S> {
    private val subject: BehaviorSubject<S> = BehaviorSubject.createDefault(initialState)

    private val disposables = CompositeDisposable()

    private val flushQueueSubject = BehaviorSubject.create<Unit>()

    private val jobs = Jobs<S>()

    override val observable: Observable<S> = subject

    override val state: S
        get() = subject.value!!

    init {

        flushQueueSubject.observeOn(Schedulers.newThread())
            .subscribe({ _ -> flushQueues() }, ::handleError)
            .registerDisposable()
    }

    override fun get(block: (S) -> Unit) {
        jobs.enqueueGetStateBlock(block)
        flushQueueSubject.onNext(Unit)
    }

    override fun set(stateReducer: S.() -> S) {
        jobs.enqueueSetStateBlock(stateReducer)
        flushQueueSubject.onNext(Unit)
    }
    private class Jobs<S> {

        private val getStateQueue = LinkedList<(state: S) -> Unit>()
        private var setStateQueue = LinkedList<S.() -> S>()

        @Synchronized
        fun enqueueGetStateBlock(block: (state: S) -> Unit) {
            getStateQueue.add(block)
        }

        @Synchronized
        fun enqueueSetStateBlock(block: S.() -> S) {
            setStateQueue.add(block)
        }

        @Synchronized
        fun dequeueGetStateBlock(): ((state: S) -> Unit)? {
            return getStateQueue.poll()
        }

        @Synchronized
        fun dequeueAllSetStateBlocks(): List<(S.() -> S)>? {
            // do not allocate empty queue for no-op flushes
            if (setStateQueue.isEmpty()) return null

            val queue = setStateQueue
            setStateQueue = LinkedList()
            return queue
        }
    }

    private fun flushQueues() {
        flushSetStateQueue()
        val block = jobs.dequeueGetStateBlock() ?: return
        block(state)
        flushQueues()
    }

    private fun flushSetStateQueue() {
        val blocks = jobs.dequeueAllSetStateBlocks() ?: return
        for (block in blocks) {
            val newState = state.block()
            if (newState != state) {
                subject.onNext(newState)
            }
        }
    }

    private fun handleError(throwable: Throwable) {
        var e: Throwable? = throwable
        while (e?.cause != null) e = e.cause
        e?.let { throw it }
    }

    override fun isDisposed() = disposables.isDisposed

    override fun dispose() {
        disposables.dispose()
    }

    private fun Disposable.registerDisposable(): Disposable {
        disposables.add(this)
        return this
    }
}