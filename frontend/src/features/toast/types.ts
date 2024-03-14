export enum ToastType {
  PRIMARY = 'primary',
  SECONDARY = 'secondary',
  SUCCESS = 'success',
  DANGER = 'danger',
  WARNING = 'warning',
  INFO = 'info',
  LIGHT = 'light',
  DARK = 'dark',
}
export interface ShowToastOptions {
  title: string;
  message: string;
  type: ToastType;
}
