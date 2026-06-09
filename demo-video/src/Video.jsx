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
  const scale = interpolate(localFrame, [0, 180], [1.025, 1], {
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
  const x = interpolate(localFrame, [0, 18], [-34, 0], {
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
      <h1 style={styles.sceneTitle}>{scene.title}</h1>
      <p style={styles.sceneSubtitle}>{scene.subtitle}</p>
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
          <div style={styles.kicker}>项目演示</div>
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
    background: '#eef3f6',
    color: '#14212f',
    fontFamily: 'Microsoft YaHei, PingFang SC, Arial, sans-serif',
    overflow: 'hidden'
  },
  header: {
    position: 'absolute',
    top: 38,
    left: 54,
    right: 54,
    height: 84,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between'
  },
  kicker: {
    color: '#1f7a76',
    fontSize: 22,
    fontWeight: 800
  },
  brand: {
    marginTop: 6,
    color: '#111827',
    fontSize: 40,
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
    padding: '10px 16px',
    fontSize: 22,
    fontWeight: 800
  },
  stage: {
    position: 'absolute',
    left: 72,
    right: 72,
    top: 150,
    bottom: 176
  },
  screen: {
    position: 'absolute',
    inset: 0,
    borderRadius: 18,
    overflow: 'hidden',
    background: '#ffffff',
    border: '1px solid #d7e0e6',
    boxShadow: '0 30px 70px rgba(15, 23, 42, 0.18)'
  },
  screenshot: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    objectPosition: 'top center',
    display: 'block'
  },
  titleBlock: {
    position: 'absolute',
    left: 34,
    bottom: 32,
    maxWidth: 720,
    borderRadius: 14,
    background: 'rgba(17, 24, 39, 0.86)',
    color: '#ffffff',
    padding: '24px 28px',
    boxShadow: '0 16px 34px rgba(15, 23, 42, 0.22)'
  },
  sceneIndex: {
    color: '#9ee1d7',
    fontSize: 20,
    fontWeight: 900,
    marginBottom: 8
  },
  sceneTitle: {
    margin: 0,
    fontSize: 44,
    lineHeight: 1.15,
    fontWeight: 900
  },
  sceneSubtitle: {
    margin: '12px 0 0',
    color: '#d9e7ea',
    fontSize: 24,
    lineHeight: 1.5,
    fontWeight: 700
  },
  caption: {
    position: 'absolute',
    left: 170,
    right: 170,
    bottom: 56,
    minHeight: 58,
    borderRadius: 12,
    background: '#ffffff',
    border: '1px solid #cbd9df',
    color: '#1f2937',
    padding: '18px 28px',
    textAlign: 'center',
    fontSize: 28,
    lineHeight: 1.45,
    fontWeight: 800,
    boxShadow: '0 14px 34px rgba(15, 23, 42, 0.12)'
  },
  progressTrack: {
    position: 'absolute',
    left: 170,
    right: 170,
    bottom: 34,
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
