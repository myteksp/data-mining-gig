import { useEffect, useState } from 'react';
import { getListUnfinishedUploads } from './../api.ts';
import { Card, ProgressBar, Table } from 'react-bootstrap';

export const ListUnfinishedUploads = () => {
  const [unfinishedUploads, setUnfinishedUploads] = useState<
    UnfinishedUpload[] | null
  >(null);

  useEffect(() => {
    getListUnfinishedUploads().then((value) => {
      setUnfinishedUploads(value.data);
    });
  }, []);

  function getPercent(value: number, total: number) {
    return (value * 100) / total;
  }

  return (
    <Card className="mb-3">
      <Card.Body>
        <Card.Title>Active ingestions</Card.Title>

        <Table striped bordered>
          <thead>
            <tr>
              <th>Filename</th>
              <th>Progress</th>
            </tr>
          </thead>
          <tbody>
            {unfinishedUploads !== null &&
              unfinishedUploads.map((item) => (
                <tr>
                  <td>{item.fileName}</td>
                  <td className={'d-flex align-items-center'}>
                    <div>
                      {((item.processed * 100) / item.outOf).toFixed(2)}%
                    </div>
                    <ProgressBar
                      animated
                      now={getPercent(item.processed, item.outOf)}
                      className={'flex-fill ms-2'}
                    />
                  </td>
                </tr>
              ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );
};
