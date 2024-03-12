import React from 'react'
import ReactDOM from 'react-dom/client'
import DropboxApp from './DropboxApp.tsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('dropbox-root')!).render(
  <React.StrictMode>
    <DropboxApp />
  </React.StrictMode>,
)
