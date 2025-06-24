package com.tangoplus.facebeauty.vm

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InputViewModel : ViewModel() {
    val nameCondition = MutableLiveData(false)
    val mobileCondition = MutableLiveData(false)
    val nameValue = MutableLiveData("")
    val mobileValue = MutableLiveData("")

    var isShownBtn = false
    var isFinishInput = false
    val inputCondition = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(nameCondition) { condition ->
            value = condition && (mobileCondition.value ?: false)
        }
        addSource(mobileCondition) { compare ->
            value = (nameCondition.value ?: false) && compare
        }
    }
}