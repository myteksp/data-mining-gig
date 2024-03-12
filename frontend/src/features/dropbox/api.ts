import { axiosInstance } from '@/features/axios';

export const getListUnfinishedUploads = () => {
  return axiosInstance.get<UnfinishedUpload[]>(
    '/uploads/listUnfinishedUploads',
  );
};
