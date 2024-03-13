import { useEffect, useState } from 'react';
import { getConnectionStatus, getConnectUrl } from './../api.ts';
import { Button } from 'react-bootstrap';

export const Connect = () => {
  const [connectionStatus, setConnectionStatus] = useState<boolean>(false);
  const [connectionUrl, setConnectionUrl] = useState(null);

  useEffect(() => {
    getConnectionStatus().then((value) => {
      setConnectionStatus(value.data.isConnected);

      if (!value.data.isConnected) {
        getConnectUrl().then((value) => {
          setConnectionUrl(value.data);
        });
      }
    });
  }, []);

  return (
    <div className={'mb-3'}>
      {connectionStatus && <div>Dropbox connected. File browser TBD</div>}
      {connectionUrl && (
        <Button href={connectionUrl} variant="outline-primary">
          Connect Dropbox
        </Button>
      )}
    </div>
  );
};
