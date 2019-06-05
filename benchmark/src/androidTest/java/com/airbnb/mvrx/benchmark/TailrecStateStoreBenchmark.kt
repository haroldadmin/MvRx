package com.airbnb.mvrx.benchmark

import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TailrecStateStoreBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val stateStore = TailrecStateStore(TestState())
    private val count = 1000

    @Test
    fun setStateTest() {
        benchmarkRule.measureRepeated {
            for (i in 1..count) {
                stateStore.set { copy(count = i) }
            }
        }
    }

    @Test
    fun simpleRecursiveCallTest() {
        benchmarkRule.measureRepeated {
            stateStore.get { state ->
                stateStore.set { copy(count = state.count + 1) }
            }
        }
    }

    @Test
    fun complexRecursiveCallTest() {
        benchmarkRule.measureRepeated {
            stateStore.get {
                stateStore.set {
                    stateStore.set { copy(count = this.count + 1) }
                    copy(count = this.count - 1)
                }
            }
            stateStore.get {
                // Just to flush the queues again
            }
        }
    }
}