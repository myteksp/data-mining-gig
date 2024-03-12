import { useEffect, useState } from 'react';
import { getListUnfinishedUploads } from './../api.ts';
import { Card, Table } from 'react-bootstrap';

export const ListUnfinishedUploads = () => {
  const [unfinishedUploads, setUnfinishedUploads] = useState<
    UnfinishedUpload[] | null
  >(null);

  useEffect(() => {
    getListUnfinishedUploads().then((value) => {
      setUnfinishedUploads(value.data);
    });
  }, []);

  useEffect(() => {
    console.log(unfinishedUploads);
  }, [unfinishedUploads]);

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
            {unfinishedUploads &&
              unfinishedUploads.map((item) => (
                <tr>
                  <td>{item.fileName}</td>
                  <td>{((item.processed * 100) / item.outOf).toFixed(2)}%</td>
                </tr>
              ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );
};
