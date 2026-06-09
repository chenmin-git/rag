import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({
  breaks: true,
  html: false,
  linkify: true,
  typographer: true
})

const defaultLinkOpen = markdown.renderer.rules.link_open || ((tokens, index, options, env, self) => self.renderToken(tokens, index, options))

markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  const targetIndex = tokens[index].attrIndex('target')
  if (targetIndex < 0) {
    tokens[index].attrPush(['target', '_blank'])
  } else {
    tokens[index].attrs[targetIndex][1] = '_blank'
  }

  const relIndex = tokens[index].attrIndex('rel')
  if (relIndex < 0) {
    tokens[index].attrPush(['rel', 'noopener noreferrer'])
  } else {
    tokens[index].attrs[relIndex][1] = 'noopener noreferrer'
  }

  return defaultLinkOpen(tokens, index, options, env, self)
}

export function renderMarkdown(value) {
  const rawHtml = markdown.render(value || '')
  return DOMPurify.sanitize(rawHtml, {
    ADD_ATTR: ['target', 'rel']
  })
}
