package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo

/**
 * 解析主题上的投票信息。
 *
 * 贴吧服务端对「未登录 / 游客」请求通常不会在 [ThreadInfo] 自身带上 `poll_info`（字段 74），
 * 但仍会下发 `origin_thread_info.poll_info`（字段 21，原贴信息，属公开数据）。
 * 为了让未登录状态也能查看投票结果与票数，这里优先取 origin_thread_info，
 * 取不到时再回退到 thread 自身的 poll_info，保证登录态行为不变。
 *
 * @param thread 主题原始数据
 * @return 投票信息；若两处都拿不到有效数据则返回 null（调用方据此决定是否显示投票卡片）
 */
internal fun resolvePollInfo(thread: ThreadInfo): PollInfo? {
    val fromOrigin = thread.origin_thread_info
        ?.poll_info
        ?.takeIf { it.options.isNotEmpty() }
    return fromOrigin ?: thread.poll_info
}
