import { Table } from 'react-bootstrap';
import { useEffect } from 'react';

interface SearchResultsParams {
  mappings: string[] | null;
  searchResults: any[];
}

export const SearchResults = (props: SearchResultsParams) => {
  const { mappings, searchResults } = props;

  useEffect(() => {
    console.log('searchResults', searchResults);
  }, [searchResults]);

  return (
    <>
      {mappings?.length && searchResults.length ? (
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
