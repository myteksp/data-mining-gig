import { Search } from './Search.tsx';
import { ListUnfinishedUploads } from './ListUnfinishedUploads.tsx';

export const Dropbox = () => {
  return (
    <div className={'main'}>
      <ListUnfinishedUploads />
      <Search />
    </div>
  );
};
