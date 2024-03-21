import { axiosInstance } from '@/features/axios';
import {
  ConnectionStatus,
  DropboxListItem,
  ExportParams,
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

  const urlParams = new URLSearchParams();
  urlParams.append('type', params.type);

  params.mappings.forEach((value) => {
    urlParams.append(`mappings`, value);
  });

  return axiosInstance.post('/uploads/upload', formData, {
    params: urlParams,
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

export const getExportToDropbox = (params: ExportParams) => {
  return axiosInstance.get('/search/exportToDropbox', {
    params: params,
  });
};

export const uploadFromDropbox = (path: string, params: UploadParams) => {
  const formData = new FormData();
  formData.append('path', path);

  const urlParams = new URLSearchParams();
  urlParams.append('type', params.type);

  params.mappings.forEach((value) => {
    urlParams.append(`mappings`, value);
  });

  return axiosInstance.post('/uploads/ingestFromDropBox', formData, {
    params: urlParams,
  });
};

export const getDropboxListFolder = (path: string) => {
  return axiosInstance.get<DropboxListItem[]>('/dropbox/listFolder', {
    params: { path: path },
  });
};
