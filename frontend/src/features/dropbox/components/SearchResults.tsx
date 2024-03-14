import { Spinner, Table } from 'react-bootstrap';
import React from 'react';

interface SearchResultsProps {
  isLoading?: boolean;
  mappings: string[] | null;
  searchResults: any[];
}

export const SearchResults: React.FC<SearchResultsProps> = ({
  isLoading,
  searchResults,
  mappings,
}) => {
  return (
    <>
      {isLoading && <Spinner animation="border" variant="primary" />}
      {!isLoading && mappings?.length && searchResults.length ? (
        <Table striped bordered hover size="sm">
          <thead>
            <tr>
              {mappings.map((item) => (
                <th key={item}>{item}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {searchResults.map((row) => (
              <tr key={row['_id']}>
                {mappings.map((item) => (
                  <td key={item} style={{ whiteSpace: 'pre' }}>
                    {row[item] ? row[item].join('\r\n') : ''}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </Table>
      ) : null}
    </>
  );
};
