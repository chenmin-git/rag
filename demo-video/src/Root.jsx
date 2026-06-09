import React from 'react'
import { Composition } from 'remotion'
import { CampusRagDemo } from './Video.jsx'

export const RemotionRoot = () => (
  <Composition
    id="CampusRagDemo"
    component={CampusRagDemo}
    durationInFrames={1800}
    fps={30}
    width={1920}
    height={1080}
  />
)
