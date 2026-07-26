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
