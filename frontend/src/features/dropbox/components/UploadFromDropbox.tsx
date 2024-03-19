import { Card, Spinner, Table } from 'react-bootstrap';
import { useEffect, useState } from 'react';
import { getDropboxListFolder } from '@/features/dropbox/api.ts';
import { DropboxListItem, DropboxListItemType } from '@/types.ts';
import { GoFileDirectoryFill } from 'react-icons/go';
import { FaRegFile } from 'react-icons/fa';
import { IoReturnUpBackSharp } from 'react-icons/io5';

export const UploadFromDropbox = () => {
  const [dropboxList, setDropboxList] = useState<DropboxListItem[]>([]);
  const [isLoadingList, setIsLoadingList] = useState(false);
  const [path, setPath] = useState<string>('');

  useEffect(() => {
    setIsLoadingList(true);
    getDropboxListFolder(path).then((value) => {
      setDropboxList(value.data);
      setIsLoadingList(false);
    });
  }, [path]);

  const goFileDirectory = (path: string) => {
    setPath(path);
  };

  const goPrevDirectory = () => {
    const prevPath = path.slice(0, path.lastIndexOf('/'));
    setPath(prevPath);
  };

  return (
    <Card>
      <Card.Body>
        <Card.Title>Ingest from Dropbox</Card.Title>

        <Table bordered hover>
          <thead>
            <tr>
              <th>Path</th>
            </tr>
          </thead>
          <tbody>
            {isLoadingList && (
              <tr>
                <td>
                  <div className="d-flex align-items-center">
                    <Spinner
                      animation="border"
                      variant="primary"
                      size="sm"
                      className={'me-2'}
                    />
                    <span>Loading...</span>
                  </div>
                </td>
              </tr>
            )}

            {!isLoadingList && path && (
              <tr>
                <td onClick={() => goPrevDirectory()} role="button">
                  <IoReturnUpBackSharp />
                  <span className={'ms-2'}>...</span>
                </td>
              </tr>
            )}

            {!isLoadingList &&
              dropboxList !== null &&
              dropboxList.map((item, index) => (
                <tr key={`dropbox-list-item-${index}`}>
                  {item.type == DropboxListItemType.DIRECTORY && (
                    <td
                      onClick={() => goFileDirectory(item.path)}
                      role="button"
                    >
                      <GoFileDirectoryFill />
                      <span className={'ms-2'}>{item.path}</span>
                    </td>
                  )}

                  {item.type == DropboxListItemType.FILE && (
                    <td role="button">
                      <FaRegFile />
                      <span className={'ms-2'}>{item.path}</span>
                    </td>
                  )}
                </tr>
              ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );
};
