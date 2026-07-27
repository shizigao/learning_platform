/**
 * 将正文相对视口的位置换算为 0–100 的阅读进度。
 *
 * @param contentTop 正文顶部相对视口顶部的位置，来自 `getBoundingClientRect()`。
 * @param contentHeight 正文完整高度。
 * @param viewportHeight 当前可视区高度。
 * @returns 四舍五入后的整数百分比；短于视口且已进入阅读区的正文直接返回 100。
 */
export function calculateReadingProgress(
  contentTop: number,
  contentHeight: number,
  viewportHeight: number,
): number {
  const viewportAnchor = Math.min(100, viewportHeight * 0.15)
  const travelDistance = contentHeight - viewportHeight + viewportAnchor
  if (travelDistance <= 0) return contentTop <= viewportAnchor ? 100 : 0
  const traveled = viewportAnchor - contentTop
  return Math.round(Math.min(100, Math.max(0, (traveled / travelDistance) * 100)))
}
