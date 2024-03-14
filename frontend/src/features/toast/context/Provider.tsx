import { ToastContext } from './Context.ts';
import React, { useState } from 'react';
import { ShowToastOptions, ToastType } from '@/features/toast/types.ts';
import { Toast, ToastContainer } from 'react-bootstrap';

interface ToastProviderProps extends React.PropsWithChildren {}
export const ToastProvider: React.FC<ToastProviderProps> = ({ children }) => {
  const [isOpened, setIsOpened] = useState(false);
  const [variant, setVariant] = useState<ToastType>(ToastType.LIGHT);
  const [title, setTitle] = useState<string>('');
  const [message, setMessage] = useState<string>('');
  const showToast = (options: ShowToastOptions) => {
    setTitle(options.title);
    setMessage(options.message);
    setVariant(options.type);
    setIsOpened(true);
  };
  return (
    <ToastContext.Provider
      value={{
        showToast,
      }}
    >
      <ToastContainer position={'top-end'}>
        <Toast
          show={isOpened}
          delay={3000}
          autohide
          onClose={() => {
            setIsOpened(false);
          }}
          /*bg={variant}*/
        >
          <Toast.Header className={`bg-${variant}`}>
            <strong className="me-auto">{title}</strong>
          </Toast.Header>
          <Toast.Body>{message}</Toast.Body>
        </Toast>
      </ToastContainer>
      {children}
    </ToastContext.Provider>
  );
};
