package com.airbnb.mvrx.benchmark

import androidx.benchmark.BenchmarkRule
import androidx.benchmark.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegularStateStoreBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private val stateStore = RegularStateStore(TestState())
    private val count = 1000

    @Test
    fun setStateTest() {
        benchmarkRule.measureRepeated {
            for (i in 1..count) {
                stateStore.set { copy(count = i) }
            }
        }
    }

}