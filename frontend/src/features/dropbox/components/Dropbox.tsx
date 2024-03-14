import { Connect } from './Connect.tsx';
import { Search } from './Search.tsx';
import { ListUnfinishedUploads } from './ListUnfinishedUploads.tsx';
import { UploadFromFile } from './UploadFromFile.tsx';

export const Dropbox = () => {
  return (
    <div className={'main'}>
      <Connect />
      <UploadFromFile />
      <ListUnfinishedUploads />
      <Search />
    </div>
  );
};
