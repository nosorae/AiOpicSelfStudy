package io.github.nosorae.aiopic.libs.workqueue.api

/** 테스트용. 작업을 실행하지 않고 넣은 순서대로 [pending] 에 쌓아 둔다. */
public class FakeWorkQueue : WorkQueue {
    private val items = mutableListOf<Pair<String, String>>()

    /** 대기 중인 (id, payload) 를 넣은 순서대로. */
    public val pending: List<Pair<String, String>> get() = items.toList()

    override suspend fun enqueue(id: String, payload: String) {
        if (items.none { it.first == id }) items += id to payload
    }

    override suspend fun cancelAll() {
        items.clear()
    }
}
