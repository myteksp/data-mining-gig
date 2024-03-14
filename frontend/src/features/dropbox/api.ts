import { axiosInstance } from '@/features/axios';
import {
  ConnectionStatus,
  SearchParams,
  UnfinishedUpload,
  UploadParams,
} from '@/types.ts';

export const getConnectionStatus = () => {
  return axiosInstance.get<ConnectionStatus>('/dropbox/connectionStatus');
};

export const getConnectUrl = () => {
  return axiosInstance.get('/dropbox/getAuthorizationUrl');
};

export const getListUnfinishedUploads = () => {
  return axiosInstance.get<UnfinishedUpload[]>(
    '/uploads/listUnfinishedUploads',
  );
};

export const upload = (file: File, params: UploadParams) => {
  const formData = new FormData();
  formData.append('file', file);

  return axiosInstance.post('/uploads/upload', formData, {
    params: params,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const getMappings = () => {
  return axiosInstance.get<string[]>('/search/getMappings');
};

export const getSearch = (params: SearchParams) => {
  return axiosInstance.get('/search/search', {
    params: params,
  });
};
