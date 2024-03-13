import { Button, Card, Col, Form, Row, Table } from 'react-bootstrap';
import { useEffect, useState } from 'react';
import { getMappings, getSearch } from './../api.ts';
import { Controller, SubmitHandler, useForm } from 'react-hook-form';
import { EnrichmentMethod, FilterType, SearchParams } from '@/types.ts';

interface SearchFormInputs {
  searchStartNode: string;
  searchQueryField: FilterType;
  searchJoinOn: string;
  searchEnrichmentMode: EnrichmentMethod;
  searchEnrichmentDepthNumber: number;
  searchSkipNumber: number;
  searchLimitNumber: number;
  searchQueryString: string;
}

export const Search = () => {
  const [mappings, setMappings] = useState<string[] | null>(null);
  const [searchResults, setSearchResults] = useState<any[]>([]);

  useEffect(() => {
    getMappings().then((value) => {
      setMappings(value.data);
      setValue('searchJoinOn', value.data[0]);
    });
  }, []);

  const { control, setValue, handleSubmit } = useForm<SearchFormInputs>({
    defaultValues: {
      searchStartNode: 'ALL',
      searchQueryField: FilterType.CONTAINS,
      searchJoinOn: '',
      searchEnrichmentMode: EnrichmentMethod.DEEP,
      searchEnrichmentDepthNumber: 10,
      searchSkipNumber: 0,
      searchLimitNumber: 100,
      searchQueryString: '',
    },
  });

  const onSubmit: SubmitHandler<SearchFormInputs> = async (data) => {
    const params: SearchParams = {
      filter: data.searchQueryString,
      filterType: data.searchQueryField,
      enrichmentMethod: data.searchEnrichmentMode,
      joinOn: data.searchJoinOn,
      maxDepth: data.searchEnrichmentDepthNumber,
      skip: data.searchSkipNumber,
      limit: data.searchLimitNumber,
    };

    if (data.searchStartNode !== 'ALL') {
      params.recordType = data.searchStartNode;
    }

    const batchSize = 10;
    const loopLimit =
      data.searchLimitNumber < batchSize
        ? 1
        : data.searchLimitNumber / batchSize;

    for (let i = 0; i < loopLimit; i++) {
      const params: SearchParams = {
        filter: data.searchQueryString,
        filterType: data.searchQueryField,
        enrichmentMethod: data.searchEnrichmentMode,
        joinOn: data.searchJoinOn,
        maxDepth: data.searchEnrichmentDepthNumber,
        skip: data.searchSkipNumber + batchSize * i,
        limit: batchSize,
      };

      if (data.searchStartNode !== 'ALL') {
        params.recordType = data.searchStartNode;
      }

      getSearch(params).then((data) => {
        setSearchResults((prevState) => {
          return [...prevState, ...data.data];
        });
        /*for (let i = 0; i < data.data.length; i++) {
          // searchResults.push(data.data[i]);
          console.log('data', data.data[i]);
          setSearchResults((prevState) => {
            return [...prevState, data.data[i]];
          });
        }*/
      });
    }
  };

  useEffect(() => {
    console.log('searchResults', searchResults);
  }, [searchResults]);

  return (
    <Card>
      <Card.Body>
        <Card.Title>Search</Card.Title>

        <Form onSubmit={handleSubmit(onSubmit)} className={'mb-3'}>
          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="search-start-node">
                <Form.Label>Select column to search</Form.Label>
                <Controller
                  name="searchStartNode"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      <option value="ALL">ALL</option>
                      {mappings !== null &&
                        mappings.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-query-field">
                <Form.Label>Select query type</Form.Label>
                <Controller
                  name="searchQueryField"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      <option value="CONTAINS">CONTAINS</option>
                      <option value="STARTS_WITH">STARTS_WITH</option>
                      <option value="ENDS_WITH">ENDS_WITH</option>
                      <option value="EQUALS">EQUALS</option>
                      <option value="NONE">NONE</option>
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-join-on">
                <Form.Label>
                  Select on which field to search commonalities
                </Form.Label>
                <Controller
                  name="searchJoinOn"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      {mappings !== null &&
                        mappings.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
          </Row>

          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="search-enrichment-mode">
                <Form.Label>Select enrichment mode</Form.Label>
                <Controller
                  name="searchEnrichmentMode"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      <option value="DEEP">DEEP</option>
                      <option value="SHALLOW">SHALLOW</option>
                      <option value="NONE">NONE</option>
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group
                className="mb-3"
                controlId="search-enrichment-depth-number"
              >
                <Form.Label>Enrichment depth</Form.Label>
                <Controller
                  name="searchEnrichmentDepthNumber"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-skip-number">
                <Form.Label>Skip</Form.Label>
                <Controller
                  name="searchSkipNumber"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
          </Row>

          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="search-limit-number">
                <Form.Label>Limit</Form.Label>
                <Controller
                  name="searchLimitNumber"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-query-string">
                <Form.Label>Query</Form.Label>
                <Controller
                  name="searchQueryString"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
          </Row>

          <Button variant="primary" type="submit">
            Search
          </Button>
        </Form>

        <Table striped bordered>
          <thead>
            <tr>
              {mappings !== null &&
                mappings.map((item) => <th key={item}>{item}</th>)}
            </tr>
          </thead>
          <tbody>
            {searchResults !== null &&
              searchResults.map((row) => (
                <tr key={row}>
                  {mappings !== null &&
                    mappings.map((item) => (
                      <td>{row[item] ? row[item].join(', ') : ''}</td>
                    ))}
                </tr>
              ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );
};
