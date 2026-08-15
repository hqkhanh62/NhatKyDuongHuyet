package com.example.nhatkyduonghuyet.ai

import java.util.ArrayDeque

class GlucoseBuffer(private val size: Int = Normalizer.SEQUENCE_LENGTH) {

    private val buffer = ArrayDeque<Float>()

    fun add(value: Float) {
        require(Normalizer.isValidGlucose(value)) { "Chỉ số đường huyết không hợp lệ." }
        if (buffer.size >= size) {
            buffer.removeFirst()
        }
        buffer.addLast(value)
    }

    fun isReady(): Boolean = buffer.size == size

    fun toInputArray(): Array<Array<FloatArray>> {
        check(isReady()) { "Chưa đủ $size chỉ số để tạo chuỗi dự đoán." }
        return Normalizer.toLstmInput(buffer.toFloatArray())
    }

    fun getAll(): List<Float> = buffer.toList()

    fun clear() {
        buffer.clear()
    }
}