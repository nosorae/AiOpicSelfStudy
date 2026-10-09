package io.github.nosorae.aiopic.packs.opicen

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 응답 클래스에서 구조화 출력 스키마를 만든다. 스키마를 손으로 따로 쓰면 응답 클래스와 어긋나서 읽기가 실패하므로
 * 클래스 하나를 기준으로 둔다. null 이 될 수 있는 필드만 required 에서 뺀다.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun jsonSchema(descriptor: SerialDescriptor): JsonObject = buildJsonObject {
    when (descriptor.kind) {
        PrimitiveKind.STRING -> put("type", "string")

        PrimitiveKind.BOOLEAN -> put("type", "boolean")

        PrimitiveKind.INT -> put("type", "integer")

        PrimitiveKind.DOUBLE -> put("type", "number")

        SerialKind.ENUM -> {
            put("type", "string")
            putJsonArray("enum") { descriptor.elementNames.forEach { add(it) } }
        }

        StructureKind.LIST -> {
            put("type", "array")
            put("items", jsonSchema(descriptor.getElementDescriptor(0)))
        }

        StructureKind.CLASS -> {
            val fields = 0 until descriptor.elementsCount
            put("type", "object")
            putJsonObject("properties") { fields.forEach { put(descriptor.getElementName(it), jsonSchema(descriptor.getElementDescriptor(it))) } }
            putJsonArray("required") { fields.filterNot { descriptor.getElementDescriptor(it).isNullable }.forEach { add(descriptor.getElementName(it)) } }
        }

        else -> error("스키마로 옮기지 않는 종류다: ${descriptor.serialName} ${descriptor.kind}")
    }
}
