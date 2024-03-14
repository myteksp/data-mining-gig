import React from 'react';
import { ToastProvider } from '@/features/toast';

interface ProvidersProps extends React.PropsWithChildren {}
export const Providers: React.FC<ProvidersProps> = ({ children }) => {
  return <ToastProvider>{children}</ToastProvider>;
};
