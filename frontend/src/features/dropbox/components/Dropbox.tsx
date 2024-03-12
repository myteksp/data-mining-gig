import { Search } from './Search.tsx';
import { ListUnfinishedUploads } from './ListUnfinishedUploads.tsx';

export const Dropbox = () => {
  return (
    <>
      <ListUnfinishedUploads />
      <Search />
      <div>Test Dropbox</div>
    </>
  );
};
