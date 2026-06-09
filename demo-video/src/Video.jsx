import React from 'react'
import {
  AbsoluteFill,
  Easing,
  Img,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig
} from 'remotion'
import scenes from './full-demo-scenes.json'
import captions from './full-demo-captions.json'

const msToFrame = (ms, fps) => Math.round((ms / 1000) * fps)

function activeByTime(items, timeMs) {
  return items.find((item) => timeMs >= item.startMs && timeMs < item.endMs) || items[items.length - 1]
}

function SceneImage({ scene, localFrame }) {
  const opacity = 1
  const scale = interpolate(localFrame, [0, 180], [1.01, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.16, 1, 0.3, 1)
  })

  return (
    <div style={styles.screen}>
      <Img
        src={staticFile(scene.image)}
        style={{
          ...styles.screenshot,
          opacity,
          transform: `scale(${scale})`
        }}
      />
    </div>
  )
}

function Caption({ caption, localFrame }) {
  const y = interpolate(localFrame, [0, 16], [26, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.16, 1, 0.3, 1)
  })
  const opacity = interpolate(localFrame, [0, 14], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp'
  })

  return (
    <div style={{ ...styles.caption, opacity, transform: `translateY(${y}px)` }}>
      {caption.text}
    </div>
  )
}

function SceneTitle({ scene, index, total, localFrame }) {
  const x = interpolate(localFrame, [0, 18], [-24, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.16, 1, 0.3, 1)
  })
  const opacity = interpolate(localFrame, [0, 14], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp'
  })

  return (
    <div style={{ ...styles.titleBlock, opacity, transform: `translateX(${x}px)` }}>
      <div style={styles.sceneIndex}>{String(index + 1).padStart(2, '0')} / {String(total).padStart(2, '0')}</div>
      <div>
        <h1 style={styles.sceneTitle}>{scene.title}</h1>
        <p style={styles.sceneSubtitle}>{scene.subtitle}</p>
      </div>
    </div>
  )
}

export function CampusRagDemo() {
  const frame = useCurrentFrame()
  const { fps } = useVideoConfig()
  const timeMs = (frame / fps) * 1000
  const scene = activeByTime(scenes, timeMs)
  const caption = activeByTime(captions, timeMs)
  const sceneIndex = scenes.findIndex((item) => item.id === scene.id)
  const sceneStartFrame = msToFrame(scene.startMs, fps)
  const captionStartFrame = msToFrame(caption.startMs, fps)
  const localSceneFrame = frame - sceneStartFrame
  const localCaptionFrame = frame - captionStartFrame
  const progress = Math.min(1, timeMs / 60000)

  return (
    <AbsoluteFill style={styles.root}>
      <div style={styles.header}>
        <div>
          <div style={styles.kicker}>高清演示</div>
          <div style={styles.brand}>校园知识库问答系统</div>
        </div>
        <div style={styles.badges}>
          <span style={styles.badge}>中文化完成</span>
          <span style={styles.badge}>构建通过</span>
          <span style={styles.badge}>测试通过</span>
        </div>
      </div>

      <div style={styles.stage}>
        <SceneImage scene={scene} localFrame={localSceneFrame} />
        <SceneTitle scene={scene} index={sceneIndex} total={scenes.length} localFrame={localSceneFrame} />
      </div>

      <div style={styles.progressTrack}>
        <div style={{ ...styles.progressBar, width: `${progress * 100}%` }} />
      </div>
      <Caption caption={caption} localFrame={localCaptionFrame} />
    </AbsoluteFill>
  )
}

const styles = {
  root: {
    background: '#ffffff',
    color: '#14212f',
    fontFamily: 'Microsoft YaHei, PingFang SC, Arial, sans-serif',
    overflow: 'hidden'
  },
  header: {
    position: 'absolute',
    top: 24,
    left: 42,
    right: 42,
    height: 72,
    display: 'none',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  kicker: {
    color: '#1f7a76',
    fontSize: 20,
    fontWeight: 800
  },
  brand: {
    marginTop: 4,
    color: '#111827',
    fontSize: 34,
    fontWeight: 900
  },
  badges: {
    display: 'flex',
    gap: 12
  },
  badge: {
    border: '1px solid #cbd9df',
    borderRadius: 8,
    background: '#ffffff',
    color: '#214352',
    padding: '8px 14px',
    fontSize: 20,
    fontWeight: 800
  },
  stage: {
    position: 'absolute',
    inset: 0,
    zIndex: 1
  },
  screen: {
    position: 'absolute',
    inset: 0,
    borderRadius: 0,
    overflow: 'hidden',
    background: '#ffffff',
    border: 'none',
    boxShadow: 'none'
  },
  screenshot: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    objectPosition: 'top center',
    display: 'block',
    background: '#ffffff'
  },
  titleBlock: {
    position: 'absolute',
    left: 258,
    top: 14,
    width: 760,
    minHeight: 62,
    borderRadius: 10,
    background: 'rgba(17, 24, 39, 0.72)',
    color: '#ffffff',
    padding: '12px 16px',
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    boxShadow: '0 12px 28px rgba(15, 23, 42, 0.16)'
  },
  sceneIndex: {
    color: '#9ee1d7',
    fontSize: 19,
    fontWeight: 900,
    minWidth: 72
  },
  sceneTitle: {
    margin: 0,
    fontSize: 28,
    lineHeight: 1.08,
    fontWeight: 900
  },
  sceneSubtitle: {
    margin: '6px 0 0',
    color: '#d9e7ea',
    fontSize: 18,
    lineHeight: 1.35,
    fontWeight: 700
  },
  caption: {
    position: 'absolute',
    left: 330,
    right: 74,
    bottom: 28,
    minHeight: 46,
    borderRadius: 10,
    background: 'rgba(255,255,255,0.94)',
    border: '1px solid #cbd9df',
    color: '#1f2937',
    padding: '14px 22px',
    textAlign: 'center',
    fontSize: 24,
    lineHeight: 1.35,
    fontWeight: 800,
    boxShadow: '0 10px 28px rgba(15, 23, 42, 0.10)'
  },
  progressTrack: {
    position: 'absolute',
    left: 330,
    right: 74,
    bottom: 12,
    height: 6,
    borderRadius: 99,
    background: '#d4e1e7',
    overflow: 'hidden'
  },
  progressBar: {
    height: '100%',
    background: '#1f7a76'
  }
}
