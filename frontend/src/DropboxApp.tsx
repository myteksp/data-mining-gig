import { Dropbox } from '@/features/dropbox';
import { Providers } from '@/providers.tsx';

function DropboxApp() {
  return (
    <Providers>
      <Dropbox />
    </Providers>
  );
}

export default DropboxApp;
