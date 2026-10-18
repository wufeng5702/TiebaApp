package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import com.huanchengfly.tieba.post.api.models.protos.PollOption
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * 投票数据解析逻辑的单元测试。
 *
 * 背景：贴吧服务端对「未登录 / 游客」请求通常不下发 [ThreadInfo.poll_info]（字段 74），
 * 但会下发 `origin_thread_info.poll_info`（字段 21）。因此解析必须优先取 origin，
 * 取不到再回退，否则未登录用户看不到投票 —— 这正是 Issue #4 的根因。
 */
class PollInfoResolverTest {

    private fun option(id: Int, num: Long) = PollOption(id = id, num = num, text = "选项$id")

    private fun pollInfo(title: String, options: List<PollOption>) = PollInfo(
        title = title,
        options = options,
        total_poll = options.sumOf { it.num },
        total_num = options.size.toLong(),
        is_multi = if (options.size > 2) 1 else 0,
    )

    // ---------- 未登录（游客）场景：Issue #4 的核心 ----------

    @Test
    fun resolvePollInfo_onlyOriginHasPoll_returnsOriginPoll() {
        val origin = pollInfo("来自原贴", listOf(option(1, 10), option(2, 20)))
        val thread = ThreadInfo(origin_thread_info = OriginThreadInfo(poll_info = origin))

        assertSame(origin, resolvePollInfo(thread))
        assertEquals("来自原贴", resolvePollInfo(thread)?.title)
    }

    @Test
    fun resolvePollInfo_originPresentButPollNull_fallsBackToOwn() {
        val own = pollInfo("自身", listOf(option(1, 5)))
        val thread = ThreadInfo(
            poll_info = own,
            origin_thread_info = OriginThreadInfo()
        )

        assertSame(own, resolvePollInfo(thread))
    }

    @Test
    fun resolvePollInfo_originPollHasNoOptions_fallsBackToOwn() {
        val own = pollInfo("自身", listOf(option(1, 7)))
        val thread = ThreadInfo(
            poll_info = own,
            origin_thread_info = OriginThreadInfo(poll_info = PollInfo(title = "退化数据"))
        )

        assertSame(own, resolvePollInfo(thread))
    }

    @Test
    fun resolvePollInfo_noPollAtAll_returnsNull() {
        assertNull(resolvePollInfo(ThreadInfo()))
    }

    // ---------- 登录态回归：确保原有行为不被破坏 ----------

    @Test
    fun resolvePollInfo_ownPollOnly_returnsOwn() {
        val own = pollInfo("自身", listOf(option(1, 3), option(2, 4)))
        val thread = ThreadInfo(poll_info = own)

        assertSame(own, resolvePollInfo(thread))
    }

    @Test
    fun resolvePollInfo_bothPresent_prefersOrigin() {
        val own = pollInfo("自身", listOf(option(1, 3)))
        val origin = pollInfo("原贴", listOf(option(9, 99), option(8, 1), option(7, 1)))
        val thread = ThreadInfo(
            poll_info = own,
            origin_thread_info = OriginThreadInfo(poll_info = origin)
        )

        assertEquals("原贴", resolvePollInfo(thread)?.title)
        assertEquals(101L, resolvePollInfo(thread)?.total_poll)
    }

    @Test
    fun resolvePollInfo_emptyOrigin_withoutOwnPoll_returnsNull() {
        val thread = ThreadInfo(origin_thread_info = OriginThreadInfo(poll_info = PollInfo()))
        assertNull(resolvePollInfo(thread))
    }
}
