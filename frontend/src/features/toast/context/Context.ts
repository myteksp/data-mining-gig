import { createContext, useContext } from 'react';
import { ShowToastOptions } from '@/features/toast/types.ts';

interface ToastContextProps {
  showToast: (options: ShowToastOptions) => void;
}
export const ToastContext = createContext<ToastContextProps>({
  showToast: () => {},
});

export const useToast = () => useContext(ToastContext);
