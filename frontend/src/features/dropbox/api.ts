import { axiosInstance } from '@/features/axios';
import { ConnectionStatus, SearchParams, UnfinishedUpload } from '@/types.ts';

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

export const getMappings = () => {
  return axiosInstance.get<string[]>('/search/getMappings');
};

export const getSearch = (params: SearchParams) => {
  return axiosInstance.get('/search/search', {
    params: params,
  });
};
