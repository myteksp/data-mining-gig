import { useEffect, useState } from 'react';
import { getConnectionStatus, getConnectUrl } from './../api.ts';
import { Badge, Button } from 'react-bootstrap';

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
    <div>
      {connectionStatus && (
        <Badge bg="success">Dropbox connected. File browser TBD</Badge>
      )}
      {connectionUrl && (
        <Button href={connectionUrl} variant="outline-info" size="sm">
          Connect Dropbox
        </Button>
      )}
    </div>
  );
};
