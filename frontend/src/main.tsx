import React from 'react';
import ReactDOM from 'react-dom/client';
import DropboxApp from './DropboxApp.tsx';
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.scss';

ReactDOM.createRoot(document.getElementById('dropbox-root')!).render(
  <React.StrictMode>
    <DropboxApp />
  </React.StrictMode>,
);
